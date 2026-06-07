# Art Source

`art-source/` — склад сырых ассетов пользователя: текстуры, звуки до обработки, архивы с референсами, скрипты перекраски, FX-спрайты и материалы, которые не являются runtime-ресурсами Minecraft.

Runtime-ассеты копируются отсюда в `src/main/resources/assets/superheroes/...` только после отбора, переименования и приведения к нужному формату.

## Что тут лежит

- `rezero-fx-textures.zip` — крупный архив с Re:Zero FX, item/entity textures, particle sheets, UI refs и моделями для Reinhard/Re:Zero-направления.
- `sung_jinwoo_v2/` — исходные текстуры Sung Jin-Woo, фаз и теней, плюс `ALL_TEXTURES.zip`.
- `raiden/` — исходный архив Blades of War и `recolor_yamato.py` для обработки Yamato/Raiden-ассетов.
- `marvel/` — исходники для Captain America / Hulkbuster (`cap_shield_face.png`, `hulkbuster_skin.png`).
- `textures/` — одиночные исходные текстуры героев и boss-вариантов.
- `sounds/` — исходные SFX, включая Homelander MP3/OGG, Reinhard voice, Thanos snap.
- `reinhard/` — отдельные исходные аудио для Reinhard.

## Правила

1. Сырые материалы остаются здесь, runtime-копии лежат в `src/main/resources/assets/superheroes/...`.
2. Перед созданием новой текстуры, звука или particle sheet сначала искать здесь.
3. Звуки для runtime — только OGG Vorbis. MP3 конвертировать:

   ```bash
   ffmpeg -i input.mp3 -c:a libvorbis -qscale:a 5 output.ogg
   ```

4. Текстуры — PNG, желательно power-of-two размеры, с альфой если нужна прозрачность.
5. Если ассет пришел из чужого пака/мода/сайта, рядом добавлять `source.txt` или `LICENSE.md`.
6. Если `art-source/` разрастется примерно выше 500 MB, обсудить git-lfs или внешний storage.

## Быстрый поиск в архивах

```bash
unzip -l art-source/rezero-fx-textures.zip | grep -i <keyword>
unzip -p art-source/rezero-fx-textures.zip "FX + TEXTURES REZERO/.../some.png" > /tmp/some.png
```

На Windows можно использовать 7-Zip или PowerShell `Expand-Archive` во временную папку, но в репозиторий переносить только реально используемые runtime-файлы.
