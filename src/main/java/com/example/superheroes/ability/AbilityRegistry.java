package com.example.superheroes.ability;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AbilityRegistry {
	private static final Map<ResourceLocation, Ability> REGISTRY = new LinkedHashMap<>();

	public static final FlightAbility FLIGHT = new FlightAbility();
	public static final EyeLasersAbility EYE_LASERS = new EyeLasersAbility();
	public static final XRayAbility X_RAY = new XRayAbility();
	public static final IronFistsAbility IRON_FISTS = new IronFistsAbility();
	public static final HandClapAbility HAND_CLAP = new HandClapAbility();
	public static final StunningRoarAbility STUNNING_ROAR = new StunningRoarAbility();
	public static final ViltrumiteChargeAbility VILTRUMITE_CHARGE = new ViltrumiteChargeAbility();
	public static final MeteorSlamAbility METEOR_SLAM = new MeteorSlamAbility();
	public static final ViltrumiteRecoveryAbility VILTRUMITE_RECOVERY = new ViltrumiteRecoveryAbility();
	public static final ShockwavePulseAbility SHOCKWAVE_PULSE = new ShockwavePulseAbility();
	public static final GuardiansBreakerAbility GUARDIANS_BREAKER = new GuardiansBreakerAbility();
	public static final OmnimanViltrumiteRushAbility OMNIMAN_VILTRUMITE_RUSH = new OmnimanViltrumiteRushAbility();
	public static final OmnimanWorldBreakerAbility OMNIMAN_WORLD_BREAKER = new OmnimanWorldBreakerAbility();
	public static final IronManFlightAbility IRON_MAN_FLIGHT = new IronManFlightAbility();
	public static final SupersonicAbility SUPERSONIC = new SupersonicAbility();
	public static final RepulsorAbility REPULSOR = new RepulsorAbility();
	public static final BoxEspAbility BOX_ESP = new BoxEspAbility();
	public static final UnibeamAbility UNIBEAM = new UnibeamAbility();
	public static final IronManHulkbusterAbility IRON_MAN_HULKBUSTER = new IronManHulkbusterAbility();

	public static final LionHeartAbility LION_HEART = new LionHeartAbility();
	public static final ManiaOfGreedAbility MANIA_OF_GREED = new ManiaOfGreedAbility();
	public static final GreedsEmbraceAbility GREEDS_EMBRACE = new GreedsEmbraceAbility();
	public static final LionRoarAbility LION_ROAR = new LionRoarAbility();
	public static final CounterStrikeAbility COUNTER_STRIKE = new CounterStrikeAbility();

	public static final AriseAbility ARISE = new AriseAbility();
	public static final ShadowExchangeAbility SHADOW_EXCHANGE = new ShadowExchangeAbility();
	public static final SacrificeAbility SACRIFICE = new SacrificeAbility();
	public static final RulersAuthorityAbility RULERS_AUTHORITY = new RulersAuthorityAbility();
	public static final ShadowExtractionAbility SHADOW_EXTRACTION = new ShadowExtractionAbility();
	public static final MonarchsDomainAbility MONARCHS_DOMAIN = new MonarchsDomainAbility();

	public static final DoomsdaySmashAbility DOOMSDAY_SMASH = new DoomsdaySmashAbility();
	public static final DoomsdayRoarAbility DOOMSDAY_ROAR = new DoomsdayRoarAbility();
	public static final DoomsdayBerserkAbility DOOMSDAY_BERSERK = new DoomsdayBerserkAbility();
	public static final DoomsdayBoneSpikeAbility DOOMSDAY_BONE_SPIKE = new DoomsdayBoneSpikeAbility();
	public static final ChargeTackleAbility DOOMSDAY_CHARGE_TACKLE = new ChargeTackleAbility();
	public static final DoomGripAbility DOOMSDAY_DOOM_GRIP = new DoomGripAbility();

	public static final GokuKamehamehaAbility GOKU_KAMEHAMEHA = new GokuKamehamehaAbility();
	public static final GokuInstantTransmissionAbility GOKU_INSTANT_TRANSMISSION = new GokuInstantTransmissionAbility();
	public static final GokuKiChargeAbility GOKU_KI_CHARGE = new GokuKiChargeAbility();
	public static final GokuSolarFlareAbility GOKU_SOLAR_FLARE = new GokuSolarFlareAbility();
	public static final GokuSpiritBombAbility GOKU_SPIRIT_BOMB = new GokuSpiritBombAbility();
	public static final GokuSuperSaiyanAuraAbility GOKU_SUPER_SAIYAN_AURA = new GokuSuperSaiyanAuraAbility();

	public static final NarutoRasenganAbility NARUTO_RASENGAN = new NarutoRasenganAbility();
	public static final NarutoShadowClonesAbility NARUTO_SHADOW_CLONES = new NarutoShadowClonesAbility();
	public static final NarutoRasenshurikenAbility NARUTO_RASENSHURIKEN = new NarutoRasenshurikenAbility();
	public static final NarutoSageModeAbility NARUTO_SAGE_MODE = new NarutoSageModeAbility();
	public static final NarutoOodamaRasenganAbility NARUTO_OODAMA_RASENGAN = new NarutoOodamaRasenganAbility();
	public static final NarutoBijuudamaAbility NARUTO_BIJUUDAMA = new NarutoBijuudamaAbility();

	public static final CapShieldThrowAbility CAP_SHIELD_THROW = new CapShieldThrowAbility();
	public static final CapShieldSlamAbility CAP_SHIELD_SLAM = new CapShieldSlamAbility();
	public static final CapShieldDashAbility CAP_SHIELD_DASH = new CapShieldDashAbility();
	public static final CapCounterStanceAbility CAP_COUNTER_STANCE = new CapCounterStanceAbility();

	public static final KratosSpartanRageAbility KRATOS_SPARTAN_RAGE = new KratosSpartanRageAbility();
	public static final KratosBladeStormAbility KRATOS_BLADE_STORM = new KratosBladeStormAbility();
	public static final KratosChainWhirlAbility KRATOS_CHAIN_WHIRL = new KratosChainWhirlAbility();
	public static final KratosLeviathanThrowAbility KRATOS_LEVIATHAN_THROW = new KratosLeviathanThrowAbility();
	public static final KratosGodSlayerAbility KRATOS_GOD_SLAYER = new KratosGodSlayerAbility();

	public static final LokiAstralClonesAbility LOKI_ASTRAL_CLONES = new LokiAstralClonesAbility();
	public static final LokiTesseractBlinkAbility LOKI_TESSERACT_BLINK = new LokiTesseractBlinkAbility();
	public static final LokiMindCharmAbility LOKI_MIND_CHARM = new LokiMindCharmAbility();
	public static final LokiGlamourAbility LOKI_GLAMOUR = new LokiGlamourAbility();
	public static final LokiChaosBoltAbility LOKI_CHAOS_BOLT = new LokiChaosBoltAbility();

	public static final ThanosCosmicSlamAbility THANOS_COSMIC_SLAM = new ThanosCosmicSlamAbility();
	public static final ThanosRealityTearAbility THANOS_REALITY_TEAR = new ThanosRealityTearAbility();
	public static final ThanosMindPulseAbility THANOS_MIND_PULSE = new ThanosMindPulseAbility();
	public static final ThanosTimeRewindAbility THANOS_TIME_REWIND = new ThanosTimeRewindAbility();
	public static final ThanosSpacePortalAbility THANOS_SPACE_PORTAL = new ThanosSpacePortalAbility();
	public static final ThanosSoulPulseAbility THANOS_SOUL_PULSE = new ThanosSoulPulseAbility();
	public static final ThanosSnapAbility THANOS_SNAP = new ThanosSnapAbility();

	public static final ReinhardSwordDrawAbility REINHARD_SWORD_DRAW = new ReinhardSwordDrawAbility();
	public static final ReinhardAirSlashAbility REINHARD_AIR_SLASH = new ReinhardAirSlashAbility();
	public static final ReinhardJudgmentMarkAbility REINHARD_JUDGMENT_MARK = new ReinhardJudgmentMarkAbility();
	public static final ReinhardWishAbility REINHARD_WISH = new ReinhardWishAbility();
	public static final ReinhardSwordWaveAbility REINHARD_SWORD_WAVE = new ReinhardSwordWaveAbility();
	public static final ReinhardCounterRiposteAbility REINHARD_COUNTER_RIPOSTE = new ReinhardCounterRiposteAbility();
	public static final ReinhardDivineAuraAbility REINHARD_DIVINE_AURA = new ReinhardDivineAuraAbility();
	public static final ReinhardSpeedJudgmentAbility REINHARD_SPEED_JUDGMENT = new ReinhardSpeedJudgmentAbility();

	public static final RaidenSwordDrawAbility RAIDEN_SWORD_DRAW = new RaidenSwordDrawAbility();
	public static final RaidenEyeOfJudgmentAbility RAIDEN_EYE_OF_JUDGMENT = new RaidenEyeOfJudgmentAbility();
	public static final RaidenMusouShinsetsuAbility RAIDEN_MUSOU_SHINSETSU = new RaidenMusouShinsetsuAbility();
	public static final RaidenPlungingStrikeAbility RAIDEN_PLUNGING_STRIKE = new RaidenPlungingStrikeAbility();
	public static final RaidenTranscendenceAbility RAIDEN_TRANSCENDENCE = new RaidenTranscendenceAbility();
	public static final RaidenMusouIsshinAbility RAIDEN_MUSOU_ISSHIN = new RaidenMusouIsshinAbility();

	public static final KazuhaChihayaburuAbility KAZUHA_CHIHAYABURU = new KazuhaChihayaburuAbility();
	public static final KazuhaMidareRanzanAbility KAZUHA_MIDARE_RANZAN = new KazuhaMidareRanzanAbility();
	public static final KazuhaAutumnWhirlwindAbility KAZUHA_AUTUMN_WHIRLWIND = new KazuhaAutumnWhirlwindAbility();
	public static final KazuhaMapleStormAbility KAZUHA_MAPLE_STORM = new KazuhaMapleStormAbility();

	public static final ScaramoucheWindstepAbility SCARAMOUCHE_WINDSTEP = new ScaramoucheWindstepAbility();
	public static final ScaramoucheElectroSwirlAbility SCARAMOUCHE_ELECTRO_SWIRL = new ScaramoucheElectroSwirlAbility();
	public static final ScaramoucheWindPrisonAbility SCARAMOUCHE_WIND_PRISON = new ScaramoucheWindPrisonAbility();
	public static final ScaramoucheSkyfallBurstAbility SCARAMOUCHE_SKYFALL_BURST = new ScaramoucheSkyfallBurstAbility();

	public static final BattleBeastPredatorLeapAbility BATTLE_BEAST_PREDATOR_LEAP = new BattleBeastPredatorLeapAbility();
	public static final BattleBeastAxeCleaveAbility BATTLE_BEAST_AXE_CLEAVE = new BattleBeastAxeCleaveAbility();
	public static final BattleBeastWarRoarAbility BATTLE_BEAST_WAR_ROAR = new BattleBeastWarRoarAbility();
	public static final BattleBeastBloodlustAbility BATTLE_BEAST_BLOODLUST = new BattleBeastBloodlustAbility();

	public static final RemMorningStarAbility REM_MORNING_STAR = new RemMorningStarAbility();
	public static final RemOniRageAbility REM_ONI_RAGE = new RemOniRageAbility();
	public static final RemHealingMagicAbility REM_HEALING_MAGIC = new RemHealingMagicAbility();
	public static final RemIceBurstAbility REM_ICE_BURST = new RemIceBurstAbility();
	public static final RemHumaIceSpikesAbility REM_HUMA_ICE_SPIKES = new RemHumaIceSpikesAbility();
	public static final RemMaceCraterAbility REM_MACE_CRATER = new RemMaceCraterAbility();
	public static final RemOniKickAbility REM_ONI_KICK = new RemOniKickAbility();

	public static final ATrainMachDashAbility A_TRAIN_MACH_DASH = new ATrainMachDashAbility();
	public static final ATrainSonicBoomAbility A_TRAIN_SONIC_BOOM = new ATrainSonicBoomAbility();
	public static final ATrainHyperspeedAbility A_TRAIN_HYPERSPEED = new ATrainHyperspeedAbility();
	public static final ATrainAdrenalineRushAbility A_TRAIN_ADRENALINE_RUSH = new ATrainAdrenalineRushAbility();

	private AbilityRegistry() {
	}

	public static void init() {
		register(FLIGHT);
		register(EYE_LASERS);
		register(X_RAY);
		register(IRON_FISTS);
		register(HAND_CLAP);
		register(STUNNING_ROAR);
		register(VILTRUMITE_CHARGE);
		register(METEOR_SLAM);
		register(VILTRUMITE_RECOVERY);
		register(SHOCKWAVE_PULSE);
		register(GUARDIANS_BREAKER);
		register(OMNIMAN_VILTRUMITE_RUSH);
		register(OMNIMAN_WORLD_BREAKER);
		register(IRON_MAN_FLIGHT);
		register(SUPERSONIC);
		register(REPULSOR);
		register(BOX_ESP);
		register(UNIBEAM);
		register(IRON_MAN_HULKBUSTER);
		register(LION_HEART);
		register(MANIA_OF_GREED);
		register(GREEDS_EMBRACE);
		register(LION_ROAR);
		register(COUNTER_STRIKE);
		register(ARISE);
		register(SHADOW_EXCHANGE);
		register(SACRIFICE);
		register(RULERS_AUTHORITY);
		register(SHADOW_EXTRACTION);
		register(MONARCHS_DOMAIN);
		register(DOOMSDAY_SMASH);
		register(DOOMSDAY_ROAR);
		register(DOOMSDAY_BERSERK);
		register(DOOMSDAY_BONE_SPIKE);
		register(DOOMSDAY_CHARGE_TACKLE);
		register(DOOMSDAY_DOOM_GRIP);
		register(GOKU_KAMEHAMEHA);
		register(GOKU_INSTANT_TRANSMISSION);
		register(GOKU_KI_CHARGE);
		register(GOKU_SOLAR_FLARE);
		register(GOKU_SPIRIT_BOMB);
		register(GOKU_SUPER_SAIYAN_AURA);
		register(NARUTO_RASENGAN);
		register(NARUTO_SHADOW_CLONES);
		register(NARUTO_RASENSHURIKEN);
		register(NARUTO_SAGE_MODE);
		register(NARUTO_OODAMA_RASENGAN);
		register(NARUTO_BIJUUDAMA);
		register(CAP_SHIELD_THROW);
		register(CAP_SHIELD_SLAM);
		register(CAP_SHIELD_DASH);
		register(CAP_COUNTER_STANCE);
		register(KRATOS_SPARTAN_RAGE);
		register(KRATOS_BLADE_STORM);
		register(KRATOS_CHAIN_WHIRL);
		register(KRATOS_LEVIATHAN_THROW);
		register(KRATOS_GOD_SLAYER);
		register(LOKI_ASTRAL_CLONES);
		register(LOKI_TESSERACT_BLINK);
		register(LOKI_MIND_CHARM);
		register(LOKI_GLAMOUR);
		register(LOKI_CHAOS_BOLT);
		register(THANOS_COSMIC_SLAM);
		register(THANOS_REALITY_TEAR);
		register(THANOS_MIND_PULSE);
		register(THANOS_TIME_REWIND);
		register(THANOS_SPACE_PORTAL);
		register(THANOS_SOUL_PULSE);
		register(THANOS_SNAP);
		register(REINHARD_SWORD_DRAW);
		register(REINHARD_AIR_SLASH);
		register(REINHARD_JUDGMENT_MARK);
		register(REINHARD_WISH);
		register(REINHARD_SWORD_WAVE);
		register(REINHARD_COUNTER_RIPOSTE);
		register(REINHARD_DIVINE_AURA);
		register(REINHARD_SPEED_JUDGMENT);
		register(RAIDEN_SWORD_DRAW);
		register(RAIDEN_EYE_OF_JUDGMENT);
		register(RAIDEN_MUSOU_SHINSETSU);
		register(RAIDEN_PLUNGING_STRIKE);
		register(RAIDEN_TRANSCENDENCE);
		register(RAIDEN_MUSOU_ISSHIN);
		register(KAZUHA_CHIHAYABURU);
		register(KAZUHA_MIDARE_RANZAN);
		register(KAZUHA_AUTUMN_WHIRLWIND);
		register(KAZUHA_MAPLE_STORM);
		register(SCARAMOUCHE_WINDSTEP);
		register(SCARAMOUCHE_ELECTRO_SWIRL);
		register(SCARAMOUCHE_WIND_PRISON);
		register(SCARAMOUCHE_SKYFALL_BURST);
		register(BATTLE_BEAST_PREDATOR_LEAP);
		register(BATTLE_BEAST_AXE_CLEAVE);
		register(BATTLE_BEAST_WAR_ROAR);
		register(BATTLE_BEAST_BLOODLUST);
		register(REM_MORNING_STAR);
		register(REM_ONI_RAGE);
		register(REM_HEALING_MAGIC);
		register(REM_ICE_BURST);
		register(REM_HUMA_ICE_SPIKES);
		register(REM_MACE_CRATER);
		register(REM_ONI_KICK);
		register(A_TRAIN_MACH_DASH);
		register(A_TRAIN_SONIC_BOOM);
		register(A_TRAIN_HYPERSPEED);
		register(A_TRAIN_ADRENALINE_RUSH);
	}

	public static void register(Ability ability) {
		REGISTRY.put(ability.getId(), ability);
	}

	@Nullable
	public static Ability get(@Nullable ResourceLocation id) {
		return id == null ? null : REGISTRY.get(id);
	}

	public static Map<ResourceLocation, Ability> all() {
		return Collections.unmodifiableMap(REGISTRY);
	}
}
