package com.github.sqshq.jsontestplugin;

import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import java.util.Optional;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class RunIntegrationTestAction extends AnAction {

  private static final String NOTIFICATION_GROUP_ID = "JsonTestRunnerNotifications";
  private static final String BAZEL_TEMPLATE_CONFIG_NAME = "Bazel Test Template";
  private static final String JSON_TEST_PREFIX_ENV = "json.test.prefix";

  private final String testTatget;
  private final boolean debug;

  public RunIntegrationTestAction(String fileName, Optional<String> testName, boolean debug) {
    this.testTatget = testName.map(s -> String.format("%s_%s", fileName, s)).orElse(fileName);
    this.debug = debug;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    var project = e.getProject();
    if (project == null) {
      return;
    }

    var runManager = RunManager.getInstance(project);
    var templateSettings = runManager.findConfigurationByName(BAZEL_TEMPLATE_CONFIG_NAME);

    if (templateSettings == null) {
      notifyError(project, "Setup Error", "Run Configuration template not found: '" + BAZEL_TEMPLATE_CONFIG_NAME + "'.");
      return;
    }

    try {
      var templateRunConfig = templateSettings.getConfiguration();
      var factory = templateRunConfig.getFactory();
      var clonedRunConfig = templateRunConfig.clone();
      var settingsToRun = runManager.createConfiguration(clonedRunConfig, factory);
      settingsToRun.setTemporary(true);
      clonedRunConfig.setName(this.testTatget);
      settingsToRun.setName(this.testTatget);

      var configuration = settingsToRun.getConfiguration();

      try {
        var tempStateElement = new Element("temp_config_state_holder");
        configuration.writeExternal(tempStateElement);

        var blazeSettingsElement = tempStateElement.getChild("blaze-settings");
        var envStateElement = blazeSettingsElement.getChild("env_state");
        if (envStateElement == null) {
          envStateElement = new Element("env_state");
          blazeSettingsElement.addContent(envStateElement);
        }

        var envsElement = envStateElement.getChild("envs");
        if (envsElement == null) {
          envsElement = new Element("envs");
          envStateElement.addContent(envsElement);
        }

        envsElement.removeContent();

        var newEnvElement = new Element("env");
        newEnvElement.setAttribute("name", JSON_TEST_PREFIX_ENV);
        newEnvElement.setAttribute("value", this.testTatget);
        envsElement.addContent(newEnvElement);

        configuration.readExternal(tempStateElement);

      } catch (WriteExternalException | InvalidDataException | ClassCastException ex) {
        notifyWarning(project, "XML Modification Failed", "Could not set ENV via XML: " + ex.getMessage());
      }

      Executor executor = this.debug ? DefaultDebugExecutor.getDebugExecutorInstance() : DefaultRunExecutor.getRunExecutorInstance();
      ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.createOrNull(executor, settingsToRun);
      if (builder == null) {
        notifyError(project, "Execution Error", "Could not create ExecutionEnvironmentBuilder for: " + settingsToRun.getName());
        return;
      }

      var environment = builder.build();
      ProgramRunnerUtil.executeConfiguration(environment, false, true);

    } catch (Exception ex) {
      notifyError(project, "Execution Error", "Failed to execute the test configuration: " + ex.getMessage());
    }
  }

  private void notifyError(Project project, String title, String content) {
    Notifications.Bus.notify(new Notification(NOTIFICATION_GROUP_ID, title, content, NotificationType.ERROR), project);
  }

  private void notifyWarning(Project project, String title, String content) {
    Notifications.Bus.notify(new Notification(NOTIFICATION_GROUP_ID, title, content, NotificationType.WARNING), project);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    var project = e.getProject();
    e.getPresentation().setEnabledAndVisible(project != null && testTatget != null && !testTatget.isEmpty());
  }
}
