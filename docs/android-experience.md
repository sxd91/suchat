# Android experience contract

## Appearance controls

The defaults are `LiquidGlass` for appearance and `Full` for performance. Both are explicit settings in **我的 → 外观**; Suchat does not choose them from device capability heuristics.

- **LiquidGlass**: full liquid-glass rendering.
- **Blur**: translucent blurred surface.
- **None**: solid Material surface.
- **Full**: enables Lens, vibrancy, animated specular highlight, and press physics.

## Navigation

The Android root tabs are **消息**, **联系人**, **发现**, and **我的**. 发现 contains 朋友圈 and 漂流瓶.

The floating bottom bar follows the WeKit liquid-glass interaction model: pill geometry, backdrop sampling, an elastic draggable selection indicator, press scale, accessibility tab semantics, and optional dynamic highlight. The icon family is Composables Material Symbols, matching WeKit.

The home side panel follows WeKit's spatial drawer model: the content shrinks up to 5%, translates right by 7dp and down by 8dp, rounds its corners, and exposes the navigation panel underneath.

## Transitions

1. Shared-element transitions are the default for routes with matching content.
2. Miuix transitions are selectable in 我的 → 外观 → 页面转场.
3. AOSP Material transitions are selectable in the same setting.
4. Reduced-motion settings replace animated transitions with short fades.
