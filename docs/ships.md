# Ship art: chassis, pieces and the composer

A ship is **a chassis plus the pieces placed over it**: the chassis is the
silhouette and every piece drawn later covers what is below it. The art lives in
plain text files, editable by hand and reloadable without rebuilding, and
`run-composer.sh` opens the composer to place the pieces and save an assembly.

Status: the composer, the file format and the colours are done; the game still
draws ships as text, and the renderer that uses these files is pending. The
piece catalogue below is the inventory of what has to be drawn.

## Files

| File | Holds | Written by |
| --- | --- | --- |
| `ships/chassis.txt` | The chassis drawings | Hand; the menu reloads it when an editor closes |
| `ships/pieces.txt` | The piece drawings | Hand; the menu reloads it when an editor closes |
| `ships/ships.txt` | One assembly: a chassis and its placed pieces | The composer, with `s` |

Lookup order (UTF-8): `ships/<file>`, `../ships/<file>`, `BeyondSpaceTraderJava/ships/<file>`.

## Format of `chassis.txt` and `pieces.txt`

```
;; a comment (only with two semicolons)
[torreta laser]              ← a part; the name may have spaces
color=cyan                   ← optional; white by default
bgcolor=black                ← optional; the colour it jumps to while blinking
blink=true                   ← optional; only true or false (false by default)
  ⦅                          ← the drawing starts here
∫.                           ← '.' is ink, a space is empty
```

- **Comments**: lines starting with `;;`. A single `;` is part of the drawing.
- **`[name]`** opens a part and closes the previous one.
- **The `[name]` of a piece is the English name of the game item** it draws
  (`[Pulse Laser]`, `[Energy Shield]`, `[Engine]`): that is how the game finds the
  piece for each thing. **In a piece**, `key=` says which site letter it fills
  (`key=M`).
- **In a chassis**, `size=` declares its size (`tiny`, `small`, `medium`, `large`,
  `huge` or `any`; empty means unknown). The ship editor warns when it does not
  match the size of the ship type, so a rowing boat cannot pass for a cargo ship
  without a warning; the hull editor sets it with `z`.
- **In a chassis**, `key=` with a style defines a **colour letter** (a free
  letter and the colour, background and blink it paints with:
  `key=X color=red bgcolor=blue blink=true`) and `zone=` paints a **colour
  zone** with it (`zone=X x=1 y=2 w=3 h=4`).
- **Keys**: `color=`, `bgcolor=`, `blink=`, lowercase, at the start of the line.
  A line starting with `Color=` is **art**, not a key.
- **Drawing**: read literally. A space is empty; everything else (dots, unicode)
  is ink. Leading and trailing spaces are kept as written.
- **Blank lines are rows of the part**, at the top and bottom too: the part
  extends over them, which is how a chassis keeps room to place pieces on. A
  blank line before the next `[name]` is one more row of the previous part.
- Width = the longest line (shorter lines are padded); height = the number of rows.
- **Blink** happens only with `blink=true` (`false` or anything else is off) and
  alternates between `color` and `bgcolor` (black when there is no `bgcolor`):
  the piece never disappears. `bgcolor` alone does nothing.

## Glyphs

The files are read as Unicode **code points**, so any glyph works, including the
ones outside the basic plane (the domino tiles 🁣 🂓, for example).

A glyph that the terminal paints **two columns wide** (emoji, CJK and, according
to Lanterna's table, the domino tiles) **takes two cells**: the composer keeps
the second one reserved for it, so the rows stay aligned. Everything else takes
one cell. Braille and the dice faces ⚀ ⚁ ⚂ ⚃ ⚄ ⚅ are single width.

### The palette (safe glyphs, copy from here)

These are the glyphs that any normal monospace setup can draw: each one is
present in at least 13 of the 20 monospace font families of the development
machine, so no fallback to a proportional font is needed. Everything else — the
technical signs, the exotic arrows, the dingbats, the dominoes — depends on the
fonts and the fallback of the reader's terminal.

Every glyph in the palette is **one cell wide**: none of them has the East Asian
Wide/Fullwidth property, so the engine never has to reserve a second cell. Two
caveats: many of them are *ambiguous* (one cell in a Western locale, two in a
terminal configured for East Asian languages), and some have an **emoji twin**.
Per the Unicode emoji data, the only palette glyph with *emoji presentation* is
⚡ (the terminal paints it as a colour emoji, two columns), so it is **out**; the
others (☀ ☁ ☂ ☎ ♠ ♥ ♦ ♣ ♀ ♂ ⚒ ⚔ ⚖ ⚗ ⚛ ☢ ☣ ⚙ © ® ™ ↔ ↕ ↖ ↗ ↘ ↙ ▪ ▫) are emoji
too but with *text* presentation, which is what terminals draw by default. If one
of them is substituted, the composer's strip (`g`) shows it.

**Letters, digits and accents**

```
A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
a b c d e f g h i j k l m n o p q r s t u v w x y z
0 1 2 3 4 5 6 7 8 9
á é í ó ú ü ñ Á É Í Ó Ú Ü Ñ ç Ç à è ì ò ù â ê î ô û ä ë ï ö ¿ ¡ ª º ¹ ² ³ ½ ¼ ¾ µ
α β γ δ ε θ λ μ π σ φ ω Α Β Γ Δ Θ Λ Π Σ Φ Ω ϟ
```

**Punctuation and signs**

```
! " # $ % & ' ( ) * + , - . / : ; < = > ? @ [ \ ] ^ _ ` { | } ~
€ ¢ £ ¥ · • … – — † ‡ ° § ¶ ¤ ¦ © ® ™
```

**Arrows**

```
←↑→↓↔↕↖↗↘↙⇐⇑⇒⇓ ⇔ ⇦ ⇧ ⇨ ⇩ ↠ ↣ ⇉ ⇝ ⇶ ⇻
```

**Maths**

```
± × ÷ ≈ ≠ ≤ ≥ ∞ √ ∑ ∏ ∫ ∂ ∆ ∇ ⌐ ∠ ∘ ¬ ⊕ ⊗ ⊙ ⊛
```

**Box drawing**

```
─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼ ═ ║ ╔ ╗ ╚ ╝ ╠ ╣ ╦ ╩ ╬
━ ┃ ┏ ┓ ┗ ┛ ┣ ┫ ┳ ┻ ╋ ┄ ┅ ┆ ┇ ┈ ┉ ┊ ┋ ╌ ╍ ╎ ╏ ╴ ╵ ╶ ╷
```

**Blocks and shades**

```
░ ▒ ▓ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▁ ▂ ▃ ▄ ▅ ▆ ▇ ▀ ▬ ▭
```

**Shapes**

```
■ □ ▪ ▫ ▲ ▼ ► ◄ ◆ ◇ ○ ● ◎ ◉ ◍ ◘ ◙ ◢ ◣ ◤ ◥
```

**Symbols**

```
★ ☆ ✦ ✧ ☀ ☁ ☂ ☎ ☏ ☼ ☽ ☾ ♠ ♥ ♦ ♣ ♤ ♡ ♢ ♧ ♪ ♫ ♬ ♭ ♯ ♩ ♀ ♂
✓ ✗ ⌂ ⚐ ⚑ ⚒ ⚔ ⚖ ⚗ ⚛ ☢ ☣ ⚙ ⌘ ⍟ ⌾ ⌁ ⌸ ⍉ ↯ ☇
```

**Dice**

```
⚀ ⚁ ⚂ ⚃ ⚄ ⚅
```

**Braille** (the whole block U+2800–U+28FF, grouped by number of dots; U+2800 is
the blank one)

```
1 dot : ⠁⠂⠄⠈⠐⠠⡀⢀
2 dots: ⠃⠅⠆⠉⠊⠌⠑⠒⠔⠘⠡⠢⠤⠨⠰⡁⡂⡄⡈⡐⡠⢁⢂⢄⢈⢐⢠⣀
3 dots: ⠇⠋⠍⠎⠓⠕⠖⠙⠚⠜⠣⠥⠦⠩⠪⠬⠱⠲⠴⠸⡃⡅⡆⡉⡊⡌⡑⡒⡔⡘⡡⡢⡤⡨⡰⢃⢅⢆⢉⢊⢌⢑⢒⢔⢘⢡⢢⢤⢨⢰⣁⣂⣄⣈⣐⣠
4 dots: ⠏⠗⠛⠝⠞⠧⠫⠭⠮⠳⠵⠶⠹⠺⠼⡇⡋⡍⡎⡓⡕⡖⡙⡚⡜⡣⡥⡦⡩⡪⡬⡱⡲⡴⡸⢇⢋⢍⢎⢓⢕⢖⢙⢚⢜⢣⢥⢦⢩⢪⢬⢱⢲⢴⢸⣃⣅⣆⣉⣊⣌⣑⣒⣔⣘⣡⣢⣤⣨⣰
5 dots: ⠟⠯⠷⠻⠽⠾⡏⡗⡛⡝⡞⡧⡫⡭⡮⡳⡵⡶⡹⡺⡼⢏⢗⢛⢝⢞⢧⢫⢭⢮⢳⢵⢶⢹⢺⢼⣇⣋⣍⣎⣓⣕⣖⣙⣚⣜⣣⣥⣦⣩⣪⣬⣱⣲⣴⣸
6 dots: ⠿⡟⡯⡷⡻⡽⡾⢟⢯⢷⢻⢽⢾⣏⣗⣛⣝⣞⣧⣫⣭⣮⣳⣵⣶⣹⣺⣼
7 dots: ⡿⢿⣟⣯⣷⣻⣽⣾
8 dots: ⣿
```

**Out of the palette** — they are drawn on this machine thanks to fallback fonts,
but a reader without those fonts would see boxes: the technical signs the piece
files use now (`⧯ ⎅ ⏚ ⏌ ⎚ ⛁ ⧎ ⦖ ⦔ ⦈ ⍡`) and the domino tiles (`🁣…🂓`).

### Extended palette (what the common monospace fonts carry)

The list above is the bullet-proof core. This one is much bigger: it is what
**Noto Sans Mono**, the primary font of the development machine, draws by
itself — so there is no fallback and, being a monospace font, every glyph is
**one cell wide by construction**. A machine without that font will reach them
through the terminal's fallback (usually another monospace font, which keeps the
cell); it is worth a look with the strip (`g`) on the target machines.

**Punctuation**

```
                      ​ ‌ ‍ ‎ ‏ ‐ ‑ ‒ – — ― ‖ ‗ ‘ ’ ‚ ‛ “ ” „ ‟ † ‡ • ‣ ․ ‥ … ‧     ‪ ‫
‬ ‭ ‮   ‰ ‱ ′ ″ ‴ ‵ ‶ ‷ ‸ ‹ › ※ ‼ ‽ ‾ ‿ ⁀ ⁁ ⁂ ⁃ ⁄ ⁅ ⁆ ⁇ ⁈ ⁉ ⁊ ⁋ ⁌ ⁍ ⁎ ⁏ ⁐ ⁑ ⁒ ⁓ ⁔ ⁕ ⁖ ⁗
⁘ ⁙ ⁚ ⁛ ⁜ ⁝ ⁞   ⁠ ⁡ ⁢ ⁣ ⁤ ⁦ ⁧ ⁨ ⁩ ⁪ ⁫ ⁬ ⁭ ⁮ ⁯
```

**Spaces**

```
                     
```

**Superscripts**

```
⁰ ⁱ ⁴ ⁵ ⁶ ⁷ ⁸ ⁹ ⁺ ⁻ ⁼ ⁽ ⁾ ⁿ ₀ ₁ ₂ ₃ ₄ ₅ ₆ ₇ ₈ ₉ ₊ ₋ ₌ ₍ ₎ ₐ ₑ ₒ ₓ ₔ ₕ ₖ ₗ ₘ ₙ ₚ ₛ ₜ
```

**Currency**

```
₠ ₡ ₢ ₣ ₤ ₥ ₦ ₧ ₨ ₩ ₪ ₫ € ₭ ₮ ₯ ₰ ₱ ₲ ₳ ₴ ₵ ₶ ₷ ₸ ₹ ₺ ₻ ₼ ₽ ₾ ₿ ⃀
```

**Letterlike**

```
℀ ℁ ℂ ℃ ℄ ℅ ℆ ℇ ℈ ℉ ℊ ℋ ℌ ℍ ℎ ℏ ℐ ℑ ℒ ℓ ℔ ℕ № ℗ ℘ ℙ ℚ ℛ ℜ ℝ ℞ ℟ ℠ ℡ ™ ℣ ℤ ℥ Ω ℧ ℨ ℩ K Å
ℬ ℭ ℮ ℯ ℰ ℱ Ⅎ ℳ ℴ ℵ ℶ ℷ ℸ ℹ ℺ ℻ ℼ ℽ ℾ ℿ ⅀ ⅁ ⅂ ⅃ ⅄ ⅅ ⅆ ⅇ ⅈ ⅉ ⅊ ⅋ ⅌ ⅍ ⅎ ⅏
```

**Numerals**

```
⅐ ⅑ ⅒ ⅓ ⅔ ⅕ ⅖ ⅗ ⅘ ⅙ ⅚ ⅛ ⅜ ⅝ ⅞ ⅟ Ↄ ↄ ↉
```

**Arrows**

```
← ↑ → ↓ ↔ ↕ ↜ ↝ ↞ ↠ ↢ ↣ ↤ ↦ ⇐ ⇑ ⇒ ⇓ ⇔ ⇚ ⇛ ⇦ ⇨
```

**Maths**

```
∀ ∁ ∂ ∃ ∄ ∅ ∆ ∇ ∈ ∉ ∊ ∋ ∌ ∍ ∎ ∐ − ∘ ∙ √ ∞ ∠ ∣ ∧ ∨ ∩ ∪ ∴ ∵ ∶ ∷ ∸ ∼ ∽ ≁ ≃ ≅ ≇ ≈ ≉ ≊ ≋ ≌ ≔
≕ ≗ ≟ ≠ ≡ ≢ ≤ ≥ ≬ ≮ ≯ ≰ ≱ ≲ ≳ ≴ ≵ ≺ ≻ ⊂ ⊃ ⊄ ⊅ ⊆ ⊇ ⊈ ⊉ ⊎ ⊑ ⊒ ⊓ ⊔ ⊕ ⊖ ⊗ ⊘ ⊙ ⊚ ⊛ ⊜ ⊢ ⊣ ⊤ ⊥
⊴ ⊵ ⊸ ⋂ ⋃ ⋄ ⋆ ⋈ ⋉ ⋊ ⋍ ⋎ ⋐ ⋑ ⋢ ⋣
```

**Technical**

```
⌈ ⌉ ⌊ ⌋ ⌐ ⌙ ⌠ ⌡ ⌶ ⌷ ⌸ ⌹ ⌺ ⌻ ⌼ ⌽ ⌾ ⌿ ⍀ ⍁ ⍂ ⍃ ⍄ ⍅ ⍆ ⍇ ⍈ ⍉ ⍊ ⍋ ⍌ ⍍ ⍎ ⍏ ⍐ ⍑ ⍒ ⍓ ⍔ ⍕ ⍖ ⍗ ⍘ ⍙
⍚ ⍛ ⍜ ⍝ ⍞ ⍟ ⍠ ⍡ ⍢ ⍣ ⍤ ⍥ ⍦ ⍧ ⍨ ⍩ ⍪ ⍫ ⍬ ⍭ ⍮ ⍯ ⍰ ⍱ ⍲ ⍳ ⍴ ⍵ ⍶ ⍷ ⍸ ⍹ ⍺ ⎕ ⎛ ⎜ ⎝ ⎞ ⎟ ⎠ ⎡ ⎢ ⎣ ⎤
⎥ ⎦ ⎧ ⎨ ⎩ ⎪ ⎫ ⎬ ⎭ ⎮ ⎰ ⎱ ⎲ ⎳ ⎴ ⎵ ⎶ ⎷ ⎸ ⎹ ⎺ ⎻ ⎼ ⎽ ⏜ ⏝ ⏞ ⏟ ⏠ ⏡
```

**Enclosed**

```
⑴ ⑵
```

**Box drawing**

```
─ ━ │ ┃ ┄ ┅ ┆ ┇ ┈ ┉ ┊ ┋ ┌ ┍ ┎ ┏ ┐ ┑ ┒ ┓ └ ┕ ┖ ┗ ┘ ┙ ┚ ┛ ├ ┝ ┞ ┟ ┠ ┡ ┢ ┣ ┤ ┥ ┦ ┧ ┨ ┩ ┪ ┫
┬ ┭ ┮ ┯ ┰ ┱ ┲ ┳ ┴ ┵ ┶ ┷ ┸ ┹ ┺ ┻ ┼ ┽ ┾ ┿ ╀ ╁ ╂ ╃ ╄ ╅ ╆ ╇ ╈ ╉ ╊ ╋ ╌ ╍ ╎ ╏ ═ ║ ╒ ╓ ╔ ╕ ╖ ╗
╘ ╙ ╚ ╛ ╜ ╝ ╞ ╟ ╠ ╡ ╢ ╣ ╤ ╥ ╦ ╧ ╨ ╩ ╪ ╫ ╬ ╭ ╮ ╯ ╰ ╱ ╲ ╳ ╴ ╵ ╶ ╷ ╸ ╹ ╺ ╻ ╼ ╽ ╾ ╿
```

**Blocks**

```
▀ ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▐ ░ ▒ ▓ ▔ ▕ ▖ ▗ ▘ ▙ ▚ ▛ ▜ ▝ ▞ ▟
```

**Shapes**

```
■ □ ▢ ▣ ▤ ▥ ▦ ▧ ▨ ▩ ▪ ▫ ▬ ▭ ▮ ▯ ▰ ▱ ▲ △ ▴ ▵ ▶ ▷ ▸ ▹ ► ▻ ▼ ▽ ▾ ▿ ◀ ◁ ◂ ◃ ◄ ◅ ◆ ◇ ◈ ◉ ◊ ○
◌ ◍ ◎ ● ◐ ◑ ◒ ◓ ◔ ◕ ◖ ◗ ◘ ◙ ◚ ◛ ◜ ◝ ◞ ◟ ◠ ◡ ◢ ◣ ◤ ◥ ◦ ◧ ◨ ◩ ◪ ◫ ◬ ◭ ◮ ◯ ◰ ◱ ◲ ◳ ◴ ◵ ◶ ◷
◸ ◹ ◺ ◻ ◼ ◿
```

**Misc symbols**

```
♭ ♮ ♯
```

**Dingbats**

```
✶ ❘ ❙ ❚
```

**Maths B**

```
⦇ ⦈ ⦣ ⦸
```

**Maths operators**

```
⨀ ⨅ ⨆
```

Anything else — the exotic arrows and technical signs, the dominoes, the cards,
the alchemical and musical symbols, the emoji, the CJK — is out of both lists,
for the reasons in **Glyphs** above.

## Colours

`#rrggbb` (exactly seven characters), a palette index `0-255`, or a **name in
English** (there are no Spanish aliases: `color=rojo` is white).

| Names | Notes |
| --- | --- |
| `black`, `red`, `green`, `yellow`, `blue`, `magenta`, `cyan` | the basic palette |
| `grey`/`gray` | white in this palette |
| `brightwhite`, `brightred`, `brightgreen`, `brightyellow`, `brightblue`, `brightmagenta`, `brightcyan` | bright variants |
| `orange`(208), `purple`(93), `pink`(218), `brown`(130), `darkgrey`(238), `lightgrey`(250), `gold`(220), `navy`(17), `teal`(30), `olive`(58), `maroon`(88), `lime`(118), `skyblue`(117) | fixed palette index |

Anything unknown falls back to white **without warning**: `color=naranja` is
white; write `color=orange` (or the index, or `#rrggbb`).

## Format of `ships.txt` (the ships)

Every section is one ship: its name, its game type (the spec that sets the site
budget), the chassis it uses and its **letter groups** (the sites the game fills).

```
;; a comment (two semicolons, like the other files)
[firefly pirata]
type=Firefly
chasis=insecto-diminuto
group=A x=3 y=1 n=2 color=red
group=M x=6 y=3
```

- `[nombre]` is free; `type=` names the ship type of the game specs (with no
  type, the panel cannot check the sites); `chasis=` must match a `[name]` of
  `chassis.txt` (`chassis=` is accepted too).
- `group=<letter> x=<n> y=<n> [n=<count>]`: a run of the same site
  letter starting at `(x,y)`, `n` letters long (one by default), in a colour.
- Coordinates: `(0,0)` is the first stored row and column of the chassis
  (leading spaces included).
- A malformed line is skipped silently, and a comment is a line starting with
  `;;` (a single `;` is never a comment).

## The editors

`run-composer.sh` opens a menu with the two editors: the **ship editor** and the
**hull editor**. Escape in an editor goes back to the menu (which reloads the
files, so the changes are there); Escape in the menu quits.

Both editors have a vertical panel on the left, and the selected row of every
list has a background of its own. The **ship editor** panel starts with the
`Ships` list: every ship shows its type in brackets (`mi nave [Bumblebee]`),
`TAB` and `⇧TAB` move through the list, the open one is highlighted and `y`
cycles its type. Under the `Elements` title and its line come the numbered
elements: `n` and `p` move through them and the keys `1` to `9` pick one
directly. Each element is a piece key the game may fill a site with
(`1. Weapon (A)`), with how many the ship has against the maximum (and a mark:
`✓` right, `⚠` missing, `✗` over, `?` without a type); then come the hull with
its colours and the same ship with the real pieces painted over it.

| Key | Action |
| --- | --- |
| `←↑→↓` or `hjkl` | Move the cursor |
| `n` / `p` | Next / previous element in the panel |
| `1` to `9` | Pick the element with that number |
| space | A letter in the cell is erased (whatever it is); an empty cell gets the element painted. The cursor moves one cell right either way |
| `v` | Preview: cycle which piece of the element is shown (the weapons, and the role markers $ / ☠ / ✶); the game uses the real loadout |
| `+` / `t` | Add a ship / rename it (title) |
| `f` / `y` | Pick the frame (chassis) from a list / cycle the type of the open ship |
| `TAB` / `⇧TAB` | Next / previous ship (the file keeps every design) |
| `s` | Save every ship back to `ships.txt` |
| `Esc` | Back to the menu |

The **hull editor** paints the colours of a hull: the drawing is read only. Its
panel lists the colour elements (`1. Cyan/Black/Blink (A)`); they are added with
`+` and removed with `-`, and removing one also erases the letters it had
painted. Then come the drawing with its colour letters and the same hull painted
with those colours.

| Key | Action |
| --- | --- |
| `←↑→↓` or `hjkl` | Move the cursor |
| `n` / `p` | Next / previous element in the panel |
| `1` to `9` | Pick the element with that number |
| space | A letter in the cell is erased (whatever it is); an empty cell gets the colour letter painted. The cursor moves one cell right either way |
| `+` / `-` | Add a colour element (the next free letter) / remove the selected one |
| `r` | Rename the selected element's letter (then type the letter) |
| `t` / `f` / `i` | Cycle its colour / background / blink |
| `z` | Cycle the size of the hull (empty, tiny, small, medium, large, huge, any) |
| `TAB` / `⇧TAB` | Previous / next hull |
| `s` | Save every edited hull back to `chassis.txt` |
| `Esc` | Back to the menu |
