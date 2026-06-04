package com.tescha.food.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// Raycast Obsidian Dark Palette (ver DESIGN.md)
// Un void casi negro donde las superficies emergen como
// estratos de carbón apenas más claros.
// ─────────────────────────────────────────────────────────────

// Núcleo de superficies (orden de profundidad)
val VoidBlack          = Color(0xFF040506) // canvas principal
val DeepCharcoal       = Color(0xFF07080A) // primera capa sobre canvas
val Graphite700        = Color(0xFF111214) // cards elevadas / modales
val Graphite600        = Color(0xFF1B1C1E) // badges, overlays
val Graphite500        = Color(0xFF363739) // bordes divisores
val Graphite400        = Color(0xFF454647) // bordes muted

// Texto / iconos
val Snow               = Color(0xFFFFFFFF) // texto primario
val Ash50              = Color(0xFFE6E6E6) // CTA primario (near-white)
val Slate200           = Color(0xFF9C9C9D) // texto secundario
val Slate300           = Color(0xFF6A6B6C) // texto terciario / iconos

// Acentos de estado y marca
val EmberRed           = Color(0xFFFF6363) // logo, status, badges rojos
val EmberDark          = Color(0xFF452324) // bg ember oscuro
val MintSignal         = Color(0xFF59D499) // estados online / éxito
val SkySignal          = Color(0xFF56C2FF) // info / info accent

// ─────────────────────────────────────────────────────────────
// Aliases retrocompatibles (las pantallas existentes referencian
// `Burgundy`/`MetallicGold`; las re-mapeamos a la paleta Raycast
// para no tener que editar cada archivo).
//   · Burgundy      → Ember Red (acento rojo distintivo)
//   · MetallicGold  → Ash 50    (near-white para acentos / precio)
// ─────────────────────────────────────────────────────────────
val Burgundy        = EmberRed
val MetallicGold    = Ash50

// Material3 surface aliases
val SurfaceDark              = VoidBlack
val SurfaceDim               = VoidBlack
val SurfaceBright            = Graphite600
val SurfaceContainerLowest   = VoidBlack
val SurfaceContainerLow      = DeepCharcoal
val SurfaceContainer         = DeepCharcoal
val SurfaceContainerHigh     = Graphite700
val SurfaceContainerHighest  = Graphite600

val OnSurfaceDark    = Snow
val OnSurfaceVariant = Slate200
val Outline          = Slate300
val OutlineVariant   = Graphite500

val Error            = Color(0xFFFFB4AB)
val OnError          = Color(0xFF690005)
val ErrorContainer   = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)

// Brand mappings para Material3 ColorScheme
val Primary             = Ash50          // CTA primario
val OnPrimary           = Color(0xFF111214)
val PrimaryContainer    = EmberRed
val OnPrimaryContainer  = Snow

val Secondary           = EmberRed
val OnSecondary         = Snow
val SecondaryContainer  = EmberDark
val OnSecondaryContainer = EmberRed

val Tertiary             = SkySignal
val OnTertiary           = VoidBlack
val TertiaryContainer    = Color(0xFF06204A)
val OnTertiaryContainer  = SkySignal

val Background    = VoidBlack
val OnBackground  = Snow
