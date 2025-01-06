## PHP Declaration Hints IntelliJ Plugin

Declaration/implementation completions for PHP class methods, driven by JSON configuration files.

Config files must be written to `<project>/.idea/phpDeclarationHints/<relative-php-file-path>.json`.

E.g. `<project>/src/Foo/Bar.php` → `<project>/.idea/phpDeclarationHints/src/Foo/Bar.php.json`

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
