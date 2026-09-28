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
| `ships/chassis.txt` | The chassis drawings | Hand; the composer reloads it with `R` |
| `ships/pieces.txt` | The piece drawings | Hand; the composer reloads it with `R` |
| `ships/ships.txt` | One assembly: a chassis and its placed pieces | The composer, with `S` |

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

## Format of `ships.txt` (one assembly)

The rules are the same as above: comments are `;;` (a single `;` is never a comment
and, here, it is ignored like any other unknown line).

```
;; a comment (two semicolons, like the other files)
[borrador]
chasis=insecto-diminuto
pieza=antena x=3 y=0 color=rojo
```

- `chasis=` must match a `[name]` of `chassis.txt`.
- `pieza=<name> x=<n> y=<n> color=<c>`: all three fields in that order, at the
  end of the line; the name may have spaces (the line is read from the right). A
  malformed line is skipped silently.
- The placement colour overrides the colour of the piece; `blink` and `bgcolor`
  come from the piece and cannot be changed per placement.
- Several `[name]` sections merge: the file holds **one** assembly (`borrador`
  by default).
- Coordinates: `(0,0)` is the first stored row and column of the chassis
  (leading spaces included). The top-left corner of the piece goes at `(x,y)`.
- Draw order = file order: the last piece is on top. The composer's `u` removes
  the last placed piece.

## The composer

`run-composer.sh`. Keys:

| Key | Action |
| --- | --- |
| `←↑→↓` | Move the cursor |
| `n` / `p` | Next / previous piece |
| `Intro`, `espacio`, `o` | Place the piece at the cursor |
| `c` | Cycle the colour of the piece to place |
| `h` | Show / hide the piece at the cursor (to see the bare chassis) |
| `g` | Glyph strip: check how the terminal paints the sample glyphs (wide ones leave a hole) |
| `u` | Undo the last placed piece |
| `x` | Empty the assembly |
| `Tab` | Next chassis |
| `r` | Reload `chassis.txt` and `pieces.txt` |
| `s` / `l` | Save / load `ships.txt` |
| `Esc`, `q` | Exit |

## What each ship can carry

Every weapon, shield and gadget takes exactly **one slot** (the ship builds one
slot per point of its spec), so the maximum number of equipment pieces is
`weapons + shields + gadgets`. Duplicates are allowed (three pulse lasers are
three pieces of the same sprite).

For sale:

| Ship | Size | Cargo | Weapons | Shields | Gadgets | Max pieces | Crew | Fuel | Hull |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Flea | Tiny | 10 | 0 | 0 | 0 | **0** | 1 | 20 | 25 |
| Gnat | Small | 15 | 1 | 0 | 1 | **2** | 1 | 14 | 100 |
| Firefly | Small | 20 | 1 | 1 | 1 | **3** | 1 | 17 | 100 |
| Mosquito | Small | 15 | 2 | 1 | 1 | **4** | 1 | 13 | 100 |
| Bumblebee | Medium | 25 | 1 | 2 | 2 | **5** | 2 | 15 | 100 |
| Beetle | Medium | 50 | 0 | 1 | 1 | **2** | 3 | 14 | 50 |
| Hornet | Large | 20 | 3 | 2 | 1 | **6** | 2 | 16 | 150 |
| Grasshopper | Large | 30 | 2 | 2 | 3 | **7** | 3 | 15 | 150 |
| Termite | Huge | 60 | 1 | 3 | 2 | **6** | 3 | 13 | 200 |
| Wasp | Huge | 35 | 3 | 2 | 2 | **7** | 3 | 14 | 200 |

Not for sale:

| Ship | Size | Cargo | Weapons | Shields | Gadgets | Max pieces | Crew | Fuel | Hull |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Space Monster | Huge | 0 | 3 | 0 | 0 | **3** | 1 | 1 | 500 |
| Dragonfly | Small | 0 | 2 | 3 | 2 | **7** | 1 | 1 | 10 |
| Mantis | Medium | 0 | 3 | 1 | 3 | **7** | 3 | 1 | 300 |
| Scarab | Large | 20 | 2 | 0 | 0 | **2** | 2 | 1 | 400 |
| Bottle | Small | 0 | 0 | 0 | 0 | **0** | 0 | 1 | 10 |
| Scorpion | Huge | 30 | 2 | 2 | 2 | **6** | 2 | 1 | 300 |
| Custom | Huge | 0 | 0 | 0 | 0 | **0** | 0 | 0 | 0 |

- The pattern holds: the bigger the chassis, the more it mounts. Small ships
  never crowd (at most 4); the 7s are Large/Huge.
- The aliens and quest ships are never fitted by the player: their art can be
  **fixed**, with the equipment baked into the chassis.
- **Cargo**: the base bays. Each *5 extra cargo bays* and each *5 hidden cargo
  bays* gadget adds 5 more (hidden ones do not show to the police).
- **No engine slots**: there is no engine upgrade in the model (the engineer is
  a crew skill). The engine bells are pure art.
- **No weapon range**: the tiers change power, price and tech level; the two
  disruptors disable systems instead.

## The variants the game distinguishes

**Weapons** (six, in tiers):

| Piece | Power | Price | Tech | Extra | Symbol |
| --- | --- | --- | --- | --- | --- |
| Pulse laser | 15 | 2,000 | t5 | — | ↠ |
| Beam laser | 25 | 12,500 | t6 | — | ⇉ |
| Military laser | 35 | 35,000 | t7 | — | ⇶ |
| Morgan's laser | 85 | 50,000 | t8 | — | ↣ |
| Photon disruptor | 20 | 15,000 | t6 | Disables systems | ⇝ |
| Quantum disruptor | 60 | 50,000 | t8 | Disables systems | ⇻ |

**Shields** (three, in tiers):

| Piece | Protection | Price | Tech | Symbol |
| --- | --- | --- | --- | --- |
| Energy shield | 100 | 5,000 | t5 | ⊙ |
| Reflective shield | 200 | 20,000 | t6 | ◉ |
| Lightning shield | 350 | 45,000 | t8 | ϟ |

**Gadgets** (seven, all different, no tiers):

| Piece | Effect | Price | Symbol |
| --- | --- | --- | --- |
| 5 extra cargo bays | +5 cargo bays | 2,500 | ⠾ |
| Auto-repair system | Improves the engineer's effect | 7,500 | ⚙ |
| Navigating system | Improves the pilot's effect | 15,000 | ◒ |
| Targeting system | Improves the fighter's effect | 25,000 | ◈ |
| Cloaking device | Pirates and police do not notice you (good engineer) | 100,000 | ⍉ |
| Fuel compactor | +3 fuel tanks | 30,000 | ▬ |
| 5 hidden cargo bays | +5 bays invisible to the police | 60,000 | ◙ |

Everything else the ship shows is **free art** (no model variant): cockpits,
wings, antennas, fuel tanks, hatches, docking rings, cargo pods, greebles. Draw
as many variants as you like; the game always uses the same one unless a rule
says otherwise.

## Cargo

One dot = one cargo bay; a braille character holds 8 dots (2x4). The gauge can be
drawn **live** from the real capacity (base + 5 per cargo gadget), so it grows
when the player installs the extra bays.

| Ship | Bays | Dots | Characters | Symbols |
| --- | --- | --- | --- | --- |
| Flea | 10 | 10 | 1¼ | ⣿⣀ |
| Gnat, Mosquito | 15 | 15 | 1⅞ | ⣿⣷ |
| Firefly, Hornet | 20 | 20 | 2½ | ⣿⣿⣤ |
| Bumblebee | 25 | 25 | 3⅛ | ⣿⣿⣿⡀ |
| Grasshopper, Scorpion | 30 | 30 | 3¾ | ⣿⣿⣿⣶ |
| Wasp | 35 | 35 | 4⅜ | ⣿⣿⣿⣿⣄ |
| Beetle | 50 | 50 | 6¼ | ⣿⣿⣿⣿⣿⣿⣀ |
| Termite | 60 | 60 | 7½ | ⣿⣿⣿⣿⣿⣿⣿⣤ |


## Mount points (proposal)

So the game can compose an actual loadout instead of a fixed picture, each
chassis declares the mounts it offers (weapon, shield, gadget) and the game
draws the chassis plus the sprite of each mounted item on its mount, in order.
The numbers above fix the budget: Tiny 0, Small 4, Medium 5, Large 7, Huge 7.
Every chassis is then drawn once with up to that many small mount positions, and
nothing else has to change when a player refits. The composer should show the
ship's budget (pending).

### Role markers

The role the game gives the ship (police, pirate or trader) is known at
encounter time, so it can be a **site** too: a `R` marker in the chassis that the
game fills with one of these three pieces.

```
[marca comerciante]
color=gold
$

[marca pirata]
color=white
☠

[marca policia]
color=red
bgcolor=blue
blink=true
✶
```

The siren alternates **red and blue** thanks to the blink: the shape switches
between `color` and `bgcolor`. The skull is emoji-capable but with text
presentation, like ★, so terminals draw it as text; the strip (`g`) tells if one
substitutes it (⚑, ⌾ and ☼ are in the palette as alternatives). The player's
ship carries no marker.

## The drawing list

**Batch 1 — the minimum to see a composed ship (9 drawings)**

- [ ] Three base chassis (small, medium, large silhouettes)
- [ ] Cockpit
- [ ] Engine nozzle
- [ ] Antenna
- [ ] Cargo pod
- [ ] Fuel tank
- [ ] One turret (pulse laser)
- [ ] One shield emitter (energy)
- [ ] One gadget (extra cargo bays)

**Batch 2 — the 16 pieces with model data**

- [ ] Pulse laser · [ ] Beam laser · [ ] Military laser · [ ] Morgan's laser
- [ ] Photon disruptor · [ ] Quantum disruptor
- [ ] Energy shield · [ ] Reflective shield · [ ] Lightning shield
- [ ] Extra cargo bays · [ ] Auto-repair · [ ] Navigating
- [ ] Targeting · [ ] Cloaking · [ ] Fuel compactor · [ ] Hidden cargo bays

**Batch 3 — fittings, lights and scene effects**

- [ ] Engine cluster · [ ] Big engine · [ ] Booster
- [ ] Wings / fins (3) · [ ] Long antennas (2) · [ ] Hull plates (3)
- [ ] Hatches (2) · [ ] Docking ring · [ ] Umbilical
- [ ] Cargo pod variants (2) · [ ] Greebles (5)
- [ ] Police siren (blinks) · [ ] Position light · [ ] Unstable reactor (blinks)
- [ ] Tribble · [ ] Alien artefact · [ ] Canister · [ ] Bottle
- [ ] Shot / beam · [ ] Impact flash · [ ] Smoke · [ ] Explosion · [ ] Sparks
- [ ] Scan line · [ ] Surrender flag · [ ] Docking line

**Special chassis** (Batch 3): space monster, Dragonfly, Mantis, Scarab,
Scorpion, Bottle.

Totals: about 60 items between pieces and effects, of which some 28 are enough
for the first playable pass (Batches 1 and 2).
