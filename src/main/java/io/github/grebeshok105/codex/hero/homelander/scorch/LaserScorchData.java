package io.github.grebeshok105.codex.hero.homelander.scorch;

import io.github.grebeshok105.codex.core.net.ScorchMark;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Per-dimension registry of laser scorch marks — the server-authoritative
 * cosmetic world state behind {@link ScorchMarksS2CPayload} sync. Stored as a
 * {@link SavedData} in the dimension's data folder, so marks survive relogs and
 * server restarts.
 *
 * <p>Marks live in a fixed ring of {@value #MAX_MARKS} entries; once full, a new
 * mark overwrites the oldest one. Admission is throttled per caster: a mark must
 * sit at least {@value #MIN_SPACING} blocks from that caster's previous mark and
 * no more than {@value #MAX_PER_SECOND} marks land per caster per second. Marks
 * whose block face stopped being solid are skipped by the client and pruned
 * lazily here — each accepted mark sweeps a bounded window of the ring and drops
 * marks on faces that are no longer sturdy.
 */
public final class LaserScorchData extends SavedData {
	public static final int MAX_MARKS = 2048;
	public static final double MIN_SPACING = 0.35;
	public static final int MAX_PER_SECOND = 6;

	private static final String DATA_NAME = "superheroes_laser_scorch";
	private static final String TAG_MARKS = "marks";
	private static final double MIN_SPACING_SQR = MIN_SPACING * MIN_SPACING;
	private static final int TICKS_PER_SECOND = 20;
	private static final int PRUNE_SCAN_PER_ADD = 64;
	private static final float MIN_SIZE = 0.25f;
	private static final float MAX_SIZE = 0.4f;

	private static final SavedData.Factory<LaserScorchData> FACTORY =
			new SavedData.Factory<>(LaserScorchData::new, LaserScorchData::load, null);

	/**
	 * Ring buffer: {@link #head} is the next write slot, which is also the oldest
	 * written slot once the buffer wrapped. Slots may be null (never written or
	 * pruned); {@link #live} counts the non-null ones. Chronological order is the
	 * physical scan starting at {@link #head} skipping nulls.
	 */
	private final ScorchMark[] marks = new ScorchMark[MAX_MARKS];
	private int head;
	private int live;
	private int pruneCursor;

	/** Transient per-caster admission state — not persisted. */
	private final Map<UUID, CasterBudget> casters = new HashMap<>();

	public static LaserScorchData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	/**
	 * Tries to stamp a scorch mark where the beam hit a block face. Returns the
	 * stored mark, or null when the face is not sturdy or the caster's
	 * spacing/rate budget rejects the hit.
	 */
	@Nullable
	public ScorchMark tryAdd(ServerLevel level, UUID caster, BlockHitResult hit) {
		BlockPos pos = hit.getBlockPos();
		Direction dir = hit.getDirection();
		if (!level.getBlockState(pos).isFaceSturdy(level, pos, dir)) {
			return null;
		}
		ScorchMark mark = buildMark(pos, dir, hit.getLocation());
		if (!accept(caster, mark, hit.getLocation(), level.getGameTime())) {
			return null;
		}
		pruneLazy(level);
		return mark;
	}

	/**
	 * Admission + insertion with no world access: the caster's spacing and
	 * per-second budget decide whether {@code mark} enters the ring.
	 */
	boolean accept(UUID caster, ScorchMark mark, Vec3 hitLocation, long gameTime) {
		CasterBudget budget = casters.computeIfAbsent(caster, c -> new CasterBudget());
		if (!budget.tryAccept(hitLocation, gameTime)) {
			return false;
		}
		insert(mark);
		return true;
	}

	/** Inserts into the ring, overwriting the oldest entry when full (public for tests). */
	public void insert(ScorchMark mark) {
		if (marks[head] == null) {
			live++;
		}
		marks[head] = mark;
		head = (head + 1) % MAX_MARKS;
		setDirty();
	}

	/** Number of live marks. */
	public int size() {
		return live;
	}

	/** Visits every live mark oldest-first. */
	public void forEachChronological(Consumer<ScorchMark> out) {
		for (int i = 0; i < MAX_MARKS; i++) {
			ScorchMark mark = marks[(head + i) % MAX_MARKS];
			if (mark != null) {
				out.accept(mark);
			}
		}
	}

	/**
	 * Bounded lazy cleanup: scans up to {@value #PRUNE_SCAN_PER_ADD} slots per
	 * accepted mark and drops marks whose face is no longer sturdy. Unloaded
	 * chunks are left alone — their block state can't be trusted.
	 */
	private void pruneLazy(ServerLevel level) {
		int scanned = 0;
		while (scanned < PRUNE_SCAN_PER_ADD && live > 0) {
			int idx = pruneCursor;
			pruneCursor = (pruneCursor + 1) % MAX_MARKS;
			scanned++;
			ScorchMark mark = marks[idx];
			if (mark == null) {
				continue;
			}
			BlockPos pos = mark.pos();
			if (!level.isLoaded(pos)) {
				continue;
			}
			if (!level.getBlockState(pos).isFaceSturdy(level, pos, mark.faceDirection())) {
				marks[idx] = null;
				live--;
				setDirty();
			}
		}
	}

	private static ScorchMark buildMark(BlockPos pos, Direction dir, Vec3 hitLocation) {
		double u;
		double v;
		switch (dir.getAxis()) {
			case X -> {
				u = hitLocation.z - pos.getZ();
				v = hitLocation.y - pos.getY();
			}
			case Y -> {
				u = hitLocation.x - pos.getX();
				v = hitLocation.z - pos.getZ();
			}
			default -> {
				u = hitLocation.x - pos.getX();
				v = hitLocation.y - pos.getY();
			}
		}
		RandomSource seeded = RandomSource.create(seedFor(pos, dir));
		float size = MIN_SIZE + seeded.nextFloat() * (MAX_SIZE - MIN_SIZE);
		int rot = seeded.nextInt(360);
		// Keep the decal fully on the face.
		float half = size * 0.5f;
		float fu = Mth.clamp((float) u, half, 1f - half);
		float fv = Mth.clamp((float) v, half, 1f - half);
		return new ScorchMark(pos, (byte) dir.get3DDataValue(), fu, fv, size, rot);
	}

	private static long seedFor(BlockPos pos, Direction dir) {
		return pos.asLong() ^ (dir.get3DDataValue() * 0x9E3779B97F4A7C15L);
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		ListTag list = new ListTag();
		forEachChronological(mark -> {
			CompoundTag entry = new CompoundTag();
			entry.putLong("pos", mark.pos().asLong());
			entry.putByte("face", mark.face());
			entry.putByte("u", (byte) ScorchMark.quantize(mark.u()));
			entry.putByte("v", (byte) ScorchMark.quantize(mark.v()));
			entry.putByte("size", (byte) ScorchMark.quantize(mark.size()));
			entry.putInt("rot", mark.rot());
			list.add(entry);
		});
		tag.put(TAG_MARKS, list);
		return tag;
	}

	/** Rebuilds a data instance from its saved tag (the {@link SavedData.Factory} loader; public for tests). */
	public static LaserScorchData load(CompoundTag tag, HolderLookup.Provider provider) {
		LaserScorchData data = new LaserScorchData();
		ListTag list = tag.getList(TAG_MARKS, Tag.TAG_COMPOUND);
		int first = Math.max(0, list.size() - MAX_MARKS);
		for (int i = first; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			data.insert(new ScorchMark(BlockPos.of(entry.getLong("pos")),
					entry.getByte("face"),
					ScorchMark.unquantize(entry.getByte("u")),
					ScorchMark.unquantize(entry.getByte("v")),
					ScorchMark.unquantize(entry.getByte("size")),
					entry.getInt("rot")));
		}
		return data;
	}

	/**
	 * Per-caster admission budget: spacing against the previous accepted mark and
	 * a sliding one-second window of at most {@value #MAX_PER_SECOND} accepts.
	 */
	static final class CasterBudget {
		private final long[] acceptTicks = new long[MAX_PER_SECOND];
		private int acceptCount;
		@Nullable
		private Vec3 lastMark;

		boolean tryAccept(Vec3 hitLocation, long gameTime) {
			if (lastMark != null && hitLocation.distanceToSqr(lastMark) < MIN_SPACING_SQR) {
				return false;
			}
			int kept = 0;
			for (int i = 0; i < acceptCount; i++) {
				if (gameTime - acceptTicks[i] < TICKS_PER_SECOND) {
					acceptTicks[kept++] = acceptTicks[i];
				}
			}
			acceptCount = kept;
			if (acceptCount == MAX_PER_SECOND) {
				return false;
			}
			acceptTicks[acceptCount++] = gameTime;
			lastMark = hitLocation;
			return true;
		}
	}
}
