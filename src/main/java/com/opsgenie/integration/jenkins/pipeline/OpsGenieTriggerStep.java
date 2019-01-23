package com.opsgenie.integration.jenkins.pipeline;

import com.opsgenie.integration.jenkins.*;
import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class OpsGenieTriggerStep extends AbstractStepImpl {

    @Nonnull
    private boolean enable;
    private boolean notifyBuildStart;
    private String tags;
    private String apiKey;
    private String apiUrl;
    private String teams;
    private String priority;
    private String buildStartPriority;

    public boolean getEnable() {
        return this.enable;
    }

    @DataBoundSetter
    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean getNotifyBuildStart() {
        return this.notifyBuildStart;
    }

    @DataBoundSetter
    public void setNotifyBuildStart(boolean notifyBuildStart) {
        this.notifyBuildStart = notifyBuildStart;
    }

    @DataBoundSetter
    public void setTags(String tags) {
        this.tags = tags;
    }

    @DataBoundSetter
    public void setTeams(String teams) {
        this.teams = teams;
    }

    public String getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getTags() {
        return tags;
    }

    public String getTeams() {
        return teams;
    }

    public String getPriority() {
        return priority;
    }

    @DataBoundSetter
    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getBuildStartPriority() {
        return buildStartPriority;
    }

    @DataBoundSetter
    public void setBuildStartPriority(String buildStartPriority) {
        this.buildStartPriority = buildStartPriority;
    }

    @DataBoundConstructor
    public OpsGenieTriggerStep() {
    }

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() {
            super(OpsGenieTriggerStepExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "opsgenie";
        }

        @Nonnull

        @Override
        public String getDisplayName() {
            return "OpsGenie step";
        }
    }

    public static class OpsGenieTriggerStepExecution extends AbstractSynchronousNonBlockingStepExecution<String> {

        private static final long serialVersionUID = 1L;

        @Inject
        transient OpsGenieTriggerStep step;

        @StepContextParameter
        transient TaskListener listener;

        @StepContextParameter
        private transient Run<?, ?> build;

        @Override
        protected String run() throws Exception {
            // default to global config values if not set in step, but allow step to
            // override all global settings
            Jenkins jenkins;
            try {
                jenkins = Jenkins.getInstance();
            } catch (NullPointerException ne) {
                listener.error("ERROR?!");
                return null;
            }
            OpsGenieNotifier.DescriptorImpl ogDesc = jenkins.getDescriptorByType(OpsGenieNotifier.DescriptorImpl.class);

            // OpsGenieNotifier notifier = new OpsGenieNotifier(step.enable,
            // step.notifyBuildStart, step.tags, step.apiKey,
            // step.apiUrl, step.teams, step.priority, step.buildStartPriority);
            // notifier.perform(build, null, (BuildListener) listener);

            // This variables for override the fields if they are not empty
            String tagsGiven = Util.fixNull(step.tags).isEmpty() ? ogDesc.getTags() : step.tags;
            String teamsGiven = Util.fixNull(step.teams).isEmpty() ? ogDesc.getTeams() : step.teams;

            AlertPriority alertPriority = AlertPriority.fromDisplayName(step.priority);
            AlertPriority notifyBuildStartPriority = AlertPriority.fromDisplayName(step.buildStartPriority);
            AlertProperties alertProperties = new AlertProperties().setTags(tagsGiven).setTeams(teamsGiven)
                    .setPriority(alertPriority).setBuildStartPriority(notifyBuildStartPriority);

            String apiKeyGiven = Util.fixNull(step.apiKey).isEmpty() ? ogDesc.getApiKey() : step.apiKey;
            String apiUrlGiven = Util.fixNull(step.apiUrl).isEmpty() ? ogDesc.getApiUrl() : step.apiUrl;

            OpsGenieNotificationRequest request = new OpsGenieNotificationRequest().setAlertProperties(alertProperties)
                    .setBuild(build).setListener(listener).setApiKey(apiKeyGiven).setApiUrl(apiUrlGiven);

            OpsGenieNotificationService ogService = new OpsGenieNotificationService(request);
            ogService.sendAfterBuildData();

            return null;
        }

    }
}
