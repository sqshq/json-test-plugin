package com.github.sqshq.jsontestplugin;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonObject;
import com.intellij.json.psi.JsonProperty;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JsonTestLineMarkerProvider extends RelatedItemLineMarkerProvider {

  private static final String TOP_LEVEL_PROPERTY = "tests";
  private static final String TEST_NAME_PROPERTY = "name";

  @Override
  protected void collectNavigationMarkers(
      @NotNull PsiElement element,
      @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result
  ) {
    if (!(element instanceof JsonProperty jsonProperty)) {
      return;
    }

    var file = element.getContainingFile();
    if (file == null || !file.getName().endsWith(".json")) {
      return;
    }

    var fileName = FilenameUtils.removeExtension(file.getName());
    var testName = Optional.<String>empty();

    if (isTestMarker(element)) {
      testName = Optional.of(((JsonStringLiteral) jsonProperty.getValue()).getValue());
    } else if (!isTopLevelMarker(element)) {
      return;
    }

    var runTestAction = new RunIntegrationTestAction(fileName, testName, false);
    runTestAction.getTemplatePresentation().setText("Run Test: " + testName.orElse(fileName));
    runTestAction.getTemplatePresentation().setIcon(AllIcons.Actions.Execute);

    var debugTestAction = new RunIntegrationTestAction(fileName, testName, true);
    debugTestAction.getTemplatePresentation().setText("Debug Test: " + testName.orElse(fileName));
    debugTestAction.getTemplatePresentation().setIcon(AllIcons.Actions.StartDebugger);

    var actionGroup = new DefaultActionGroup();
    actionGroup.add(runTestAction);
    actionGroup.add(debugTestAction);

    GutterIconNavigationHandler<PsiElement> navigationHandler = (MouseEvent e, PsiElement elt) -> {
      DataContext dataContext = DataManager.getInstance().getDataContext(e.getComponent());
      JBPopupFactory.getInstance()
          .createActionGroupPopup(
              "Run/Debug Test",
              actionGroup,
              dataContext,
              JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
              false,
              ActionPlaces.EDITOR_GUTTER_POPUP
          )
          .show(new RelativePoint(e));
    };

    var markerInfo = new RelatedItemLineMarkerInfo<>(
        jsonProperty.getNameElement(),
        jsonProperty.getNameElement().getTextRange(),
        AllIcons.Actions.Execute,
        object -> "Run/Debug Test",
        navigationHandler,
        GutterIconRenderer.Alignment.LEFT,
        List::of
    );

    result.add(markerInfo);
  }

  private boolean isTestMarker(@NotNull PsiElement element) {
    if (!(element instanceof JsonProperty nameProperty)) {
      return false;
    }

    if (!TEST_NAME_PROPERTY.equals(nameProperty.getName())) {
      return false;
    }

    var nameValue = nameProperty.getValue();
    if (!(nameValue instanceof JsonStringLiteral)) {
      return false;
    }

    var testNameString = ((JsonStringLiteral) nameValue).getValue();
    if (testNameString.trim().isEmpty()) {
      return false;
    }

    var testObject = nameProperty.getParent();
    if (!(testObject instanceof JsonObject)) {
      return false;
    }

    var testArray = testObject.getParent();
    if (!(testArray instanceof JsonArray)) {
      return false;
    }

    var testsPropertyElement = testArray.getParent();
    if (!(testsPropertyElement instanceof JsonProperty testsProperty)) {
      return false;
    }

    return TOP_LEVEL_PROPERTY.equals(testsProperty.getName());
  }

  private boolean isTopLevelMarker(@NotNull PsiElement element) {
    if (!(element instanceof JsonProperty testsProperty)) {
      return false;
    }

    if (!TOP_LEVEL_PROPERTY.equals(testsProperty.getName())) {
      return false;
    }

    if (!(testsProperty.getValue() instanceof JsonArray)) {
      return false;
    }

    var parentObject = testsProperty.getParent();
    if (!(parentObject instanceof JsonObject)) {
      return false;
    }

    return (parentObject.getParent() instanceof PsiFile);
  }

  @Nullable
  @Override
  public String getName() {
    return "Json test run/debug line marker";
  }
}
