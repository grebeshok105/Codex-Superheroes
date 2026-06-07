---
description: Разобрать краш-репорт мода
---

1. Прочитай `run/crash-reports/crash-YYYY-MM-DD...txt` или `run/logs/latest.log`
2. Найди `Caused by:` или `Exception in thread`
3. Определи: это мой код, чужой мод, или vanilla?
4. Если мой код — найди строчку из stacktrace, покажи и предложи фикс
5. Если чужой мод — проверь совместимость версий, предложи workaround или mixin
6. Проверь после фикса: запусти `/run-client`
