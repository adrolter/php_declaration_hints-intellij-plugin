## PHP Declaration Hints IntelliJ Plugin

Provides declaration/implementation completions for PHP class methods, driven by (typically generated) JSON configuration files.

### Config files
[JSON Schema](src/main/resources/config.schema.json)

#### Location

**`<project>/.idea/phpDeclarationHints/<relative-php-file-path>.json`**

E.g.: `<project>/src/Foo/Bar.php` → `<project>/.idea/phpDeclarationHints/src/Foo/Bar.php.json`

#### Example
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

### TODO
- [ ] Create action for configs garbage collection, and invoke it as a background task at startup
- [ ] Display error output when `fromJson()` throws (JSON schema validation in catch?)
- [ ] Purge configs from memoization when their PHP files are closed in the editor
- [ ] Create reference/example PHP script(s) for config generation
- [ ] Improve priority handling
  - https://intellij-support.jetbrains.com/hc/en-us/community/posts/115000646804-Completion-Provider-with-Priority-Sort
  - https://intellij-support.jetbrains.com/hc/en-us/community/posts/360009877039-Prioritizing-Code-Completion
