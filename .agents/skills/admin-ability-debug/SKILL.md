---
name: admin-ability-debug
description: Use when adding, changing, or reviewing admin/debug support that lets abilities normally limited to player targets temporarily affect mobs for testing in the Superheroes Minecraft mod.
---

# Admin Ability Debug

Use this skill for features where an ability that normally targets players needs a temporary admin/debug path for mobs. Treat this as a test harness, not gameplay balance.

## Core principles

- Keep debug mob targeting off by default and available only for testing by admins.
- Do not turn debug behavior into a permanent balance change. Normal survival, PvP, cooldown, resource, and damage rules stay canonical unless the user explicitly asks for a balance change.
- Do not mix PvP behavior and mob-debug behavior in the same condition. Keep player targeting as the primary path and put mob support behind a clearly named debug check.
- Add a toggle command, not a hidden constant. Include enable, disable, and status feedback so admins can see the current state.
- Check permissions server-side before any toggle or debug-only action. Use the repository's established command permission style, or a conservative operator/admin permission level if there is no local helper.
- Show status clearly to the command sender, and make failure states explicit: disabled, no permission, unsupported ability, invalid target, or server-only command.
- Keep clean server-side authority. The server decides whether debug targeting is enabled, whether the user is allowed, and whether the target is valid. Client packets may request normal actions, but must not grant debug access.

## Implementation workflow

1. Read the existing ability and identify its normal target contract. Preserve the player-target path first.
2. Add or extend a central debug controller/config instead of scattering global booleans through abilities.
3. Register a server-side command for toggling and checking status. Prefer the existing mod command namespace if one exists.
4. Add per-ability checks so only explicitly opted-in abilities can use mob-debug targeting.
5. Update localization for every new command/status/error string in both `en_us.json` and `ru_ru.json`.
6. Build after code changes with `./gradlew build --no-daemon -x test`.
7. Run a manual global checklist in a dev world or dedicated-server style environment when behavior is target-sensitive.

## Recommended shape

Create a central controller or config class for admin ability debug state. It should answer questions like:

- Is debug mob targeting enabled on this server?
- Is this command source/player allowed to use it?
- Is this ability opted in?
- Is this target eligible for the debug path?

Then keep ability code narrow:

```java
if (target instanceof ServerPlayer targetPlayer) {
    // Existing PvP/player behavior.
} else if (target instanceof Mob mob && AdminAbilityDebugController.canTargetMob(caster, abilityId, mob)) {
    // Debug-only mob behavior.
}
```

The debug branch should call shared effect application helpers where possible, but the authorization and target-type gate should remain obvious at the call site.

## Command guidance

Provide a clear toggle command, for example under the mod's existing command namespace:

- `... admin ability-debug mobs enable`
- `... admin ability-debug mobs disable`
- `... admin ability-debug mobs status`

Use exact command names that fit the existing command tree. Always require permission for enable/disable/status if status reveals admin-only config. Return localized messages that name the mode and resulting state.

## Per-ability checks

Do not make every player-targeting ability affect mobs automatically. Add explicit opt-in through one of these patterns, favoring local conventions:

- A central set of ability ids that support debug mob targeting.
- A small helper method per ability, if ability-specific target rules matter.
- A marker/interface only if the codebase already uses that style.

Keep unsupported abilities silent in normal gameplay and explicit in admin debug flows.

## Localization

For new user-visible text, update both runtime lang files:

- `src/main/resources/assets/superheroes/lang/en_us.json`
- `src/main/resources/assets/superheroes/lang/ru_ru.json`

Prefer keys that make the debug/admin nature visible, such as command status, enabled, disabled, no permission, and unsupported ability messages.

## Manual global checklist

Before calling the work done, verify:

- Default state is disabled.
- Non-admin players cannot enable, disable, or use debug mob targeting.
- Admins can enable, disable, and query status.
- Normal PvP/player targeting still behaves exactly as before.
- Mobs are affected only when debug mode is enabled and the ability opted in.
- Disabling the toggle immediately restores normal behavior.
- Client-only changes cannot bypass server checks.
- Feedback is visible and localized for both English and Russian.
- `./gradlew build --no-daemon -x test` passes.

