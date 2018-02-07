# jbpm-notifications
jBPM notification addon to allow to send and receive emails as integration with user tasks

How to use it
--------------------

Clone or download this repository and build it locally. It might take a while depending if you already have code of KIE projects.
Once built, copy following jar files into your KIE Server war (kie-server.war/WEB-INF/lib)
- jbpm-notifications-api/target/jbpm-notifications-api-0.0.1-SNAPSHOT.jar
- jbpm-notifications-impl/target/jbpm-notifications-impl-0.0.1-SNAPSHOT.jar
- jbpm-notifications-kieserver/target/jbpm-notifications-kieserver-0.0.1-SNAPSHOT.jar

**Configure user info **
since the main part of integration is with user task, you need to provide email address information for your users used by task service. This is done via UserInfo configuration. For most basic one (like trying it out or demo) you can simply create userinfo.properties file in kie-server.war/WEB-INF/classes with following content:

```
john=john@gmail.com\:en-UK\:john
Administrator=administrator@domain.com:en-UK:Administrator
HR=hr@domain.com:en-UK:HR:[john]
```

you can add as many users (or groups) into that file, make sure that users that you use in your processes are defined there.

You can configure other implementation of user info by selecting one of the available types via system proeprty and additional configuration, see jBPM docs for more details.


**Configure email service**

If you would like to have global configuration of email integration (meaning will be shared by all kjars) then create file named
email-service.properties in kie-server.war/WEB-INF/classes with following content:

```
host=imap.googlemail.com
port=993
smtp.host=smtp.gmail.com
smtp.port=587
smtp.from=
smtp.replyto=
username=
password=
inbox.folder=INBOX
domain=jbpm.org
mailSession=mail/jbpmMailSession
```

this is a sample config for gmail, so make sure you set the values properly for the mail server you use, all of the properties are mandatory.

If you prefer (which is actually recommended) to use email service per kjar, then create kjar-email-service.properties in the root folder (src/main/resources) of your kjar with exact same content. That way you can have different kjars listening and sending with different email account.

kjar can also include *-email.ftl or *-email.html that will be used as email templates for your user tasks notifications. Template name (part of the template file without -email.ftl/html) is given on user task via TaskName property (same that is used by forms).


**Configure notification listener for user tasks**
Last element is to add task event listener that is responsible for sending notifications for tasks. It is done via deployment descriptor and is as follows:

```
<task-event-listeners>
    <task-event-listener>
        <resolver>mvel</resolver>
        <identifier>new org.jbpm.extensions.notifications.impl.listeners.NotificationTaskEventListener()</identifier>
        <parameters/>
    </task-event-listener>
</task-event-listeners>
```

By default, email notification is sent only for tasks that:
- have actual owner set (this is mainly to avoid any race conditions between potential owners)
- have subject set on user task - to avoid sending notifications for all user tasks.

This behavior can be changed by extending NotificationTaskEventListener and modifying parts of it to the way you need it.


Example project
====================

Here is a project that you can directly clone build and deploy to KIE Server that will illustrate few features:
https://github.com/mswiderski/bpm-projects/tree/master/assistant

take a look at this article to know more on how it works under the hood.
http://mswiderski.blogspot.com/2018/02/interact-with-your-processes-via-email.html