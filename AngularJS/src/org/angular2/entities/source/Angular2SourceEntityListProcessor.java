// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.entities.source;

import com.intellij.lang.ecmascript6.psi.ES6ImportSpecifierAlias;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptClass;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptSingleType;
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptFunctionCachingVisitor;
import com.intellij.lang.javascript.psi.ecmal4.JSClass;
import com.intellij.lang.javascript.psi.impl.JSFunctionBaseImpl;
import com.intellij.lang.javascript.psi.impl.JSFunctionCachedData;
import com.intellij.lang.javascript.psi.impl.JSFunctionNodesVisitor;
import com.intellij.lang.javascript.psi.types.JSAnyType;
import com.intellij.lang.javascript.psi.types.JSGenericTypeImpl;
import com.intellij.lang.javascript.psi.types.JSTypeSource;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.AstLoadingFilter;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.angular2.entities.Angular2EntitiesProvider;
import org.angular2.entities.Angular2Entity;
import org.angular2.entities.Angular2Module;
import org.angular2.entities.metadata.psi.Angular2MetadataFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.util.ObjectUtils.doIfNotNull;
import static com.intellij.util.ObjectUtils.tryCast;
import static java.util.Arrays.asList;
import static org.angular2.entities.Angular2ModuleResolver.NG_MODULE_PROP;

public abstract class Angular2SourceEntityListProcessor<T extends Angular2Entity> {

  private final Class<T> myEntityClass;
  private final boolean myAcceptNgModuleWithProviders;
  private final JSElementVisitor myResultsVisitor = new JSElementVisitor() {
    @Override
    public void visitJSClass(JSClass aClass) {
      T entity = getEntity(aClass);
      if (entity != null) {
        processEntity(entity);
      }
      else {
        processNonEntityClass(aClass);
      }
    }

    @Override
    public void visitJSArrayLiteralExpression(JSArrayLiteralExpression node) {
      //it's ok, if array does not have any children
    }

    @Override
    public void visitJSFunctionDeclaration(JSFunction node) {
      resolveFunctionReturnType(node);
    }

    @Override
    public void visitJSFunctionExpression(JSFunctionExpression node) {
      resolveFunctionReturnType(node);
    }

    @Override
    public void visitJSElement(JSElement node) {
      processAnyElement(node);
    }
  };

  public Angular2SourceEntityListProcessor(@NotNull Class<T> entityClass) {
    myEntityClass = entityClass;
    myAcceptNgModuleWithProviders = entityClass.isAssignableFrom(Angular2Module.class);
  }

  protected final List<PsiElement> resolve(PsiElement t) {
    SmartList<PsiElement> result = new SmartList<>();
    t.accept(createResolveVisitor(result));
    return result;
  }

  private JSElementVisitor createResolveVisitor(SmartList<PsiElement> result) {
    return new JSElementVisitor() {
      @Override
      public void visitJSArrayLiteralExpression(JSArrayLiteralExpression node) {
        result.addAll(asList(node.getExpressions()));
      }

      @Override
      public void visitJSObjectLiteralExpression(JSObjectLiteralExpression node) {
        if (myAcceptNgModuleWithProviders) {
          AstLoadingFilter.forceAllowTreeLoading(node.getContainingFile(), () ->
            ContainerUtil.addIfNotNull(result, doIfNotNull(node.findProperty(NG_MODULE_PROP),
                                                           JSProperty::getValue)));
        }
      }

      @Override
      public void visitJSReferenceExpression(JSReferenceExpression node) {
        ContainerUtil.addIfNotNull(result, node.resolve());
      }

      @Override
      public void visitJSVariable(JSVariable node) {
        AstLoadingFilter.forceAllowTreeLoading(node.getContainingFile(), () ->
          ContainerUtil.addIfNotNull(result, node.getInitializer()));
      }

      @Override
      public void visitES6ImportSpecifierAlias(ES6ImportSpecifierAlias specifierAlias) {
        ContainerUtil.addIfNotNull(result, specifierAlias.findAliasedElement());
      }

      @Override
      public void visitJSSpreadExpression(JSSpreadExpression spreadExpression) {
        ContainerUtil.addIfNotNull(result, spreadExpression.getExpression());
      }

      @Override
      public void visitJSCallExpression(JSCallExpression node) {
        ContainerUtil.addIfNotNull(result, node.getStubSafeMethodExpression());
      }
    };
  }

  protected final JSElementVisitor getResultsVisitor() {
    return myResultsVisitor;
  }

  protected void processNonEntityClass(@NotNull JSClass aClass) {

  }

  protected void processEntity(@NotNull T entity) {

  }

  protected void processAnyType() {

  }

  protected void processAnyElement(JSElement node) {

  }

  private void resolveFunctionReturnType(@NotNull JSFunction function) {
    Set<JSResolvedTypeId> visitedTypes = new HashSet<>();
    boolean lookingForModule = myEntityClass.isAssignableFrom(Angular2Module.class);
    JSClass resolvedClazz = null;
    if (lookingForModule) {
      Angular2MetadataFunction metadataFunction = Angular2EntitiesProvider.findMetadataFunction(function);
      if (metadataFunction != null
          && metadataFunction.getReferencedModule() != null) {
        //noinspection unchecked
        processEntity((T)metadataFunction.getReferencedModule());
        return;
      }
    }
    JSType type = function.getReturnType();
    while (type != null
           && !(type instanceof JSAnyType)
           && visitedTypes.add(type.getResolvedTypeId())) {
      NotNullLazyValue<JSRecordType> recordType = NotNullLazyValue.createValue(type::asRecordType);
      JSRecordType.PropertySignature ngModuleSignature;
      if (lookingForModule
          && (ngModuleSignature = recordType.getValue().findPropertySignature(NG_MODULE_PROP)) != null) {
        type = evaluateModuleWithProvidersType(ngModuleSignature, type.getSource());
      }
      else {
        PsiElement sourceElement = type.getSourceElement();
        if (sourceElement instanceof TypeScriptSingleType) {
          JSReferenceExpression expression = AstLoadingFilter.forceAllowTreeLoading(
            sourceElement.getContainingFile(), ((TypeScriptSingleType)sourceElement)::getReferenceExpression);
          if (expression != null) {
            resolvedClazz = tryCast(expression.resolve(), JSClass.class);
            T entity = getEntity(resolvedClazz);
            if (entity != null) {
              processEntity(entity);
              return;
            }
          }
        }
        else if (sourceElement instanceof TypeScriptClass) {
          resolvedClazz = (JSClass)sourceElement;
          T entity = getEntity(resolvedClazz);
          if (entity != null) {
            processEntity(entity);
            return;
          }
        }
        JSRecordType.CallSignature constructor = ContainerUtil.find(recordType.getValue().getCallSignatures(),
                                                                    JSRecordType.CallSignature::hasNew);
        type = doIfNotNull(constructor, JSRecordType.CallSignature::getReturnType);
      }
    }
    if (resolvedClazz != null && !(type instanceof JSAnyType)) {
      processNonEntityClass(resolvedClazz);
    }
    else {
      processAnyType();
    }
  }

  private static JSType evaluateModuleWithProvidersType(JSRecordType.PropertySignature ngModuleSignature, JSTypeSource functionTypeSource) {
    JSType result = ngModuleSignature.getJSType();
    List<JSType> args;
    JSFunctionBaseImpl function;

    if (result instanceof JSGenericTypeImpl
        && (args = ((JSGenericTypeImpl)result).getArguments()).size() == 1
        && args.get(0) instanceof JSAnyType
        && functionTypeSource.getSourceElement() != null
        && (function = tryCast(functionTypeSource.getSourceElement().getContext(), JSFunctionBaseImpl.class)) != null) {

      JSType evaluatedReturnType = CachedValuesManager.getCachedValue(function, () -> {
        final JSFunctionCachedData cachedData = new JSFunctionCachedData();
        final List<JSFunction> nestedFuns = new SmartList<>();
        final List<JSType> evaluatedReturnTypes = new SmartList<>();
        final JSFunctionNodesVisitor cachedDataEvaluator =
          new TypeScriptFunctionCachingVisitor(function,
                                               cachedData, nestedFuns, evaluatedReturnTypes);
        AstLoadingFilter.forceAllowTreeLoading(
          function.getContainingFile(),
          () -> cachedDataEvaluator.visitElement(function.getNode()));
        return CachedValueProvider.Result.create(cachedDataEvaluator.getReturnTypeFromEvaluated(), function);
      });

      ngModuleSignature = doIfNotNull(evaluatedReturnType,
                                      t -> t.asRecordType().findPropertySignature(NG_MODULE_PROP));
      result = doIfNotNull(ngModuleSignature, JSRecordType.PropertySignature::getJSType);
    }
    return result;
  }

  private T getEntity(@Nullable JSClass aClass) {
    return tryCast(Angular2EntitiesProvider.getEntity(aClass), myEntityClass);
  }
}
