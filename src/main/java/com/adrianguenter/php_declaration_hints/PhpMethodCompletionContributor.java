package com.adrianguenter.php_declaration_hints;

import com.adrianguenter.php_declaration_hints.config.PhpMethodProviderConfig;
import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.GroupStatementImpl;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpClassFieldsListImpl;
import com.jetbrains.php.refactoring.move.constant.PhpMoveFileConstantProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class PhpMethodCompletionContributor
        extends CompletionContributor {
    public PhpMethodCompletionContributor() {
        this.extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet resultSet
                    ) {
                        var element = parameters.getOriginalPosition();
                        if (element == null) {
                            return;
                        }

                        var file = element.getContainingFile();
                        var project = file.getProject();
                        var configRepository = project.getService(ConfigRepository.class);
                        var templateManager = TemplateManager.getInstance(project);

                        var phpClass = PsiTreeUtil.getParentOfType(element, PhpClass.class);
                        if (phpClass == null) {
                            return;
                        }

                        MethodImpl method;
                        if (element.getParent() instanceof MethodImpl) {
                            method = (MethodImpl) element.getParent();
                        } else if (element instanceof PsiWhiteSpace && element.getPrevSibling() instanceof MethodImpl) {
                            /// Capture the method if we're immediately after one that's unnamed.
                            /// E.g.: `public function <caret>`, but not `public function foo(){} <caret>`
                            method = ((MethodImpl) element.getPrevSibling()).getNameIdentifier() == null
                                    ? (MethodImpl) element.getPrevSibling()
                                    : null;
                        } else if (
                                element instanceof PsiWhiteSpace
                                        && element.getPrevSibling() instanceof PhpClassFieldsListImpl
                                        && (!(element.getPrevSibling().getLastChild() instanceof LeafPsiElement)
                                        || !((LeafPsiElement) element.getPrevSibling().getLastChild()).getElementType().toString().equals("semicolon"))
                        ) {
                            ///  TODO: Fix `public int $a, <caret>` (should not hint!)
                            return;
                        } else if (element.getParent() instanceof PhpClass) {
                            method = null;
                        } else {
                            return;
                        }

                        var config = configRepository.get(file.getVirtualFile());
                        if (config == null || !config.classes().containsKey(phpClass.getFQN())) {
                            return;
                        }

                        var phpStyle = CodeStyle.getLanguageSettings(file, PhpLanguage.INSTANCE);
                        var classConfig = config.classes().get(phpClass.getFQN());

                        Function<@NotNull String, String> basename = (@NotNull String fqcn) -> {
                            int lastSeparatorIdx = fqcn.lastIndexOf("\\");

                            return lastSeparatorIdx != -1
                                    ? fqcn.substring(lastSeparatorIdx + 1)
                                    : fqcn;
                        };

                        for (var methodProvider : classConfig.methodProviders().entrySet()) {
                            var methodName = methodProvider.getKey();
                            var methodConfig = methodProvider.getValue();

                            if (phpClass.findOwnMethodByName(methodName) != null) {
                                continue;
                            }

                            var accessLevel = methodConfig.accessLevel();

                            if (method != null) {
                                if (method.isStatic() && !methodConfig.isStatic()) {
                                    continue;
                                }

                                if (!accessLevel.equals(method.getAccess())) {
                                    continue;
                                }
                            }

                            String shortParamsText;
                            shortParamsText = String.join(
                                    ", ",
                                    methodConfig.params().entrySet().stream().map(
                                            item -> String.format(
                                                    "%s %s$%s%s",
                                                    basename.apply(item.getValue().type()),
                                                    item.getValue().isVariadic()
                                                            ? "..."
                                                            : "",
                                                    item.getKey(),
                                                    item.getValue().defaultValue() != null
                                                            ? " = " + item.getValue().defaultValue()
                                                            : ""
                                            )
                                    ).toList()
                            );

                            var accessLevelText = accessLevel.toString();

                            var staticText = methodConfig.isStatic()
                                    ? " static"
                                    : "";

                            var returnTypeText = methodConfig.returnType();

                            LookupElementBuilder lookupElementBuilder = LookupElementBuilder
                                    .create(methodName)
                                    .withIcon(PhpIcons.METHOD)
//                                    .withIcon(PhpIcons.OVERRIDES)
                                    .withPresentableText(accessLevelText + staticText + " function " + methodName)
                                    .withTailText("(" + shortParamsText + ") {...}", true)
                                    .withTypeText(basename.apply(returnTypeText), true)
                                    .withInsertHandler((insertionContext, lookupElement) -> {
                                        Editor editor = insertionContext.getEditor();
                                        Document document = editor.getDocument();

                                        var existingDocComment = method != null ? method.getDocComment() : null;
                                        var groupStatementText = getGroupStatementText(method, methodConfig);

                                        TextRange methodTextRange = getTextRange(method, existingDocComment);

                                        if (methodTextRange != null) {
                                            document.deleteString(methodTextRange.getStartOffset(), methodTextRange.getEndOffset());
                                            editor.getCaretModel().moveToOffset(methodTextRange.getStartOffset());
                                        } else {
                                            document.deleteString(insertionContext.getStartOffset(), insertionContext.getTailOffset());
                                            editor.getCaretModel().moveToOffset(insertionContext.getStartOffset());
                                        }

                                        var attributesText = "";
                                        if (method != null) {
                                            attributesText = String.join(
                                                    "\n",
                                                    method.getAttributes()
                                                            .stream().map(attr -> "#[" + attr.getText() + "]")
                                                            .toList()
                                            ) + "\n";
                                        }

                                        var commentText = "";
                                        if (existingDocComment != null) {
                                            commentText = existingDocComment.getText();
                                        } else if (methodConfig.comment() != null) {
                                            commentText = methodConfig.comment();
                                        }

                                        var paramsOnNewLine = phpStyle.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE
                                                && !methodConfig.params().isEmpty();

                                        String paramsText;
                                        paramsText = String.join(
                                                ", ",
                                                methodConfig.params().entrySet().stream().map(
                                                        param -> String.format(
                                                                "%s %s$%s%s",
                                                                param.getValue().type(),
                                                                param.getValue().isVariadic()
                                                                        ? "..."
                                                                        : "",
                                                                param.getKey(),
                                                                param.getValue().defaultValue() != null
                                                                        ? " = " + param.getValue().defaultValue()
                                                                        : ""
                                                        )
                                                ).toList()
                                        );

                                        Template template = templateManager.createTemplate(
                                                "",
                                                "",
                                                String.format(
                                                        "%s%s%s%s function %s(%s%s): %s\n{\n%s}",
                                                        commentText,
                                                        attributesText,
                                                        accessLevelText,
                                                        staticText,
                                                        methodName,
                                                        paramsOnNewLine ? "\n" : "",
                                                        paramsText,
                                                        returnTypeText,
                                                        !groupStatementText.isEmpty()
                                                                ? groupStatementText
                                                                : "$END$\n"
                                                )
                                        );
                                        ///  TODO: Figure out what template.setToShortenLongNames() and template.setToIndent() do (and why they seem to have no effect)
                                        ///  TODO: Figure out why `phpClass.findOwnMethodByName(methodName)` is `null` (when method is `null`) without `setToReformat(true)`
                                        template.setToReformat(true);

                                        templateManager.startTemplate(editor, template);

                                        /// This should hold the same reference as `method` (unless `method` is `null`)
                                        var writtenMethod = phpClass.findOwnMethodByName(methodName);
                                        if (writtenMethod == null) {
                                            throw new RuntimeException("Method not found after insertion");
                                        }

                                        try {
                                            var references = ContainerUtil.union(
                                                    PhpPsiUtil.findChildrenNonStrict(writtenMethod, ClassReference.class),
                                                    ContainerUtil.union(
                                                            PhpPsiUtil.findChildrenNonStrict(writtenMethod, FunctionReference.class),
                                                            PhpPsiUtil.findChildrenNonStrict(writtenMethod, ConstantReference.class)
                                                    )
                                            );

                                            for (var reference : references) {
                                                PhpMoveFileConstantProcessor.replaceReferenceWithResolvedImport(
                                                        project,
                                                        writtenMethod,
                                                        reference
                                                );
                                            }
                                        } catch (Exception ignored) {
                                            ///  TODO: Handle/log
                                        }
                                    });

                            if (methodConfig.priority() != null) {
                                resultSet.addElement(PrioritizedLookupElement.withPriority(
                                        lookupElementBuilder,
                                        methodConfig.priority()
                                ));
                            } else {
                                resultSet.addElement(lookupElementBuilder);
                            }
                        }
                    }

                    private static @Nullable TextRange getTextRange(
                            MethodImpl method,
                            PhpDocComment existingDocComment
                    ) {
                        @Nullable TextRange methodTextRange;
                        if (method == null) {
                            methodTextRange = null;
                        } else if (existingDocComment != null) {
                            methodTextRange = new TextRange(
                                    existingDocComment.getTextOffset(),
                                    method.getTextRange().getEndOffset()
                            );
                        } else {
                            methodTextRange = method.getTextRange();
                        }
                        return methodTextRange;
                    }

                    private static @NotNull String getGroupStatementText(
                            MethodImpl method,
                            PhpMethodProviderConfig methodConfig
                    ) {
                        var methodLastChild = method != null ? method.getLastChild() : null;
                        var existingGroupStatement = methodLastChild instanceof GroupStatementImpl
                                ? methodLastChild
                                : null;

                        var groupStatementText = "";
                        if (existingGroupStatement != null) {
                            groupStatementText = existingGroupStatement.getText()
                                    /// Remove braces
                                    .substring(1, existingGroupStatement.getText().length() - 1)
                                    .trim();
                        }

                        if (groupStatementText.isEmpty() && methodConfig.impl() != null) {
                            groupStatementText = methodConfig.impl();
                        }

                        if (!groupStatementText.contains("$END$")) {
                            groupStatementText += "$END$\n";
                        }
                        return groupStatementText;
                    }
                }
        );
    }
}
