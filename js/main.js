/* Codex Superheroes — site interactions */
(function () {
  "use strict";

  /* ── ability icons marquee ── */
  var ICONS = ["a_train_adrenaline_rush","a_train_hyperspeed","a_train_mach_dash","a_train_sonic_boom","arise","battle_beast_axe_cleave","battle_beast_bloodlust","battle_beast_predator_leap","battle_beast_war_roar","cap_counter_stance","cap_shield_dash","cap_shield_slam","cap_shield_throw","counter_strike","doomsday_berserk","doomsday_bone_spike","doomsday_charge_tackle","doomsday_doom_grip","doomsday_roar","doomsday_smash","eye_lasers","flight","goku_instant_transmission","goku_kamehameha","goku_ki_charge","goku_solar_flare","goku_spirit_bomb","goku_super_saiyan_aura","greeds_embrace","guardians_breaker","hand_clap","iron_fists","iron_man_flight","iron_man_legion","iron_man_nano_form","iron_man_suit_switch","kazuha_autumn_whirlwind","kazuha_chihayaburu","kazuha_maple_storm","kazuha_midare_ranzan","kratos_blade_storm","kratos_chain_whirl","kratos_god_slayer","kratos_leviathan_throw","kratos_spartan_rage","lion_heart","lion_roar","loki_astral_clones","loki_chaos_bolt","loki_glamour","loki_mind_charm","loki_tesseract_blink","mania_of_greed","monarchs_domain","naruto_bijuudama","naruto_oodama_rasengan","naruto_rasengan","naruto_rasenshuriken","naruto_sage_mode","omniman_think_mark","omniman_viltrumite_rush","omniman_world_breaker","raiden_eye_of_judgment","raiden_musou_isshin","raiden_musou_shinsetsu","raiden_plunging_strike","raiden_sword_draw","raiden_transcendence","reinhard_air_slash","reinhard_counter_riposte","reinhard_divine_aura","reinhard_judgment_mark","reinhard_speed_judgment","reinhard_sword_draw","reinhard_sword_wave","reinhard_wish","scorpion_spear","scorpion_hellfire","scorpion_fire_teleport","scorpion_hell_breath","rem_healing_magic","rem_huma_ice_spikes","rem_ice_burst","rem_mace_crater","rem_morning_star","rem_oni_kick","rem_oni_rage","repulsor","rulers_authority","sacrifice","scaramouche_electro_swirl","scaramouche_skyfall_burst","scaramouche_wind_prison","scaramouche_windstep","shadow_exchange","shadow_extraction","stunning_roar","supersonic","thanos_cosmic_slam","thanos_mind_pulse","thanos_reality_tear","thanos_snap","thanos_soul_pulse","thanos_space_portal","thanos_time_rewind","unibeam","viltrumite_charge","viltrumite_recovery","x_ray"];

  function buildMarquee(el, icons) {
    var frag = document.createDocumentFragment();
    // duplicate list once → translateX(-50%) loops seamlessly
    [icons, icons].forEach(function (list) {
      list.forEach(function (name) {
        var d = document.createElement("div");
        d.className = "tile";
        var img = document.createElement("img");
        img.src = "img/abilities/" + name + ".png";
        img.alt = name.replace(/_/g, " ");
        img.loading = "lazy";
        d.appendChild(img);
        frag.appendChild(d);
      });
    });
    el.appendChild(frag);
  }
  var half = Math.ceil(ICONS.length / 2);
  var m1 = document.getElementById("mq1"), m2 = document.getElementById("mq2");
  if (m1) buildMarquee(m1, ICONS.slice(0, half));
  if (m2) buildMarquee(m2, ICONS.slice(half));

  /* ── nav scrolled state ── */
  var nav = document.getElementById("nav");
  function onScroll() { nav.classList.toggle("scrolled", window.scrollY > 30); }
  window.addEventListener("scroll", onScroll, { passive: true });
  onScroll();

  /* ── reveal on scroll ── */
  var io = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) {
      if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
    });
  }, { threshold: 0.12, rootMargin: "0px 0px -40px 0px" });
  document.querySelectorAll(".rv").forEach(function (el, i) {
    el.style.transitionDelay = (i % 4) * 70 + "ms";
    io.observe(el);
  });

  /* ── animated counters ── */
  var cio = new IntersectionObserver(function (entries) {
    entries.forEach(function (e) {
      if (!e.isIntersecting) return;
      cio.unobserve(e.target);
      var el = e.target, target = +el.dataset.count, suffix = el.dataset.suffix || "";
      var t0 = null;
      function step(ts) {
        if (!t0) t0 = ts;
        var p = Math.min((ts - t0) / 1400, 1);
        el.textContent = Math.round(target * (1 - Math.pow(1 - p, 3))) + suffix;
        if (p < 1) requestAnimationFrame(step);
      }
      requestAnimationFrame(step);
    });
  }, { threshold: 0.6 });
  document.querySelectorAll("[data-count]").forEach(function (el) { cio.observe(el); });

  /* ── hero parallax ── */
  var heroImg = document.querySelector(".hero-bg img");
  if (heroImg && matchMedia("(prefers-reduced-motion: no-preference)").matches) {
    window.addEventListener("scroll", function () {
      var y = window.scrollY;
      if (y < window.innerHeight) heroImg.style.transform = "scale(1.05) translateY(" + y * 0.25 + "px)";
    }, { passive: true });
  }
})();
