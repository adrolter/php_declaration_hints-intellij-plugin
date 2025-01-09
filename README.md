# <sub><img src="./src/main/resources/META-INF/pluginIcon.svg"></sub><sup>PHP Declaration Hints IntelliJ Plugin</sup>

Provides declaration/implementation completions for PHP class methods, driven by (typically generated) JSON configuration files.

> 🔗&nbsp;&nbsp;[View on JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26274-php-declaration-hints)

## Config files

### Location

**`<project>/.idea/phpDeclarationHints/<relative-php-file-path>.json`**

E.g.: `<project>/src/Foo/Bar.php` → `<project>/.idea/phpDeclarationHints/src/Foo/Bar.php.json`

### [JSON Schema](src/main/resources/config.schema.json)

### Example
```json
{
    "autoDelete": true,
    "classes" : {
        "\\Foo\\Bar": {
            "methodProviders" : {
                "methodA" : {
                    "isStatic": true,
                    "accessLevel": "private",
                    "returnType": "float",
                    "comment" : "/**\nMy description\n\n@param list<int> $a Some integer values\n*/",
                    "params": {
                        "a": {
                            "type": "int",
                            "isVariadic": true
                        }
                    },
                    "impl": "$result = \\array_sum($a) / $END$;\n\n/** Call biz for important reasons */\nself::biz();\n\nreturn $result;"
                },
                "methodB": {
                    "priority": 1000,
                    "params": {
                        "a": {},
                        "b": {
                            "type": "int",
                            "defaultValue": "3"
                        }
                    }
                },
                "methodC" : {}
            }
        }
    }
}
```

## TODO
- [ ] Create action for configs garbage collection, and invoke it as a background task at startup
- [ ] Display error output when `fromJson()` throws (JSON schema validation in catch?)
- [ ] Purge configs from memoization when their PHP files are closed in the editor
- [ ] Create reference/example PHP script(s) for config generation
- [ ] Improve priority handling
  - https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000646804-Completion-Provider-with-Priority-Sort
  - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009877039-Prioritizing-Code-Completion
