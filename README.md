This service posts a message to the *stock-trader-messsages* channel on the ibm-cloud@slack.com server on **Slack**.

This service expects a **JSON** object in the http body, containing the following fields: *id*, *owner*, *old*, and *new*.  It returns a **JSON** object containing the message sent (or any error message from the attempt) and the location (**Slack**).

There is another implementation of this service called *notification-twitter*, which sends the message to **Twitter** channel instead.  If both implmentations of the *Notification* service are installed, you could use **Istio** *routing rules* to determine which gets used, and under what conditions.  An example Istio route rule is in this repository (and another is in the *notification-twitter* repository).
