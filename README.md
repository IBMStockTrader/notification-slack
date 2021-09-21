This service posts a message to the *stock-trader-messsages* channel on the ibm-cloud@slack.com server on **Slack**, via a call to a serverless function.  Depending on the URL that is configured, it either invokes an IBM Cloud Functions (aka Apache OpenWhisk) function, or an AWS Lambda function.  Either way expects the same input JSON and produces the same result.

This service expects a **JSON** object in the http body, containing the following fields: *id*, *owner*, *old*, and *new*.  It returns a **JSON** object containing the message sent (or any error message from the attempt) and the location (**Slack**).

There is another implementation of this service called *notification-twitter*, which sends the message to **Twitter** instead.  If both implmentations of the *Notification* service are installed, you could use **Istio** *routing rules* to determine which gets used, and under what conditions.  An example Istio route rule is in this repository (and another is in the *notification-twitter* repository).

### Deploy

Use WebSphere Liberty helm chart to deploy Slack Notification microservice:
```bash
helm repo add ibm-charts https://raw.githubusercontent.com/IBM/charts/master/repo/stable/
helm install ibm-charts/ibm-websphere-liberty -f <VALUES_YAML> -n <RELEASE_NAME> --tls
```

In practice this means you'll run something like:
```bash
helm repo add ibm-charts https://raw.githubusercontent.com/IBM/charts/master/repo/stable/
helm install ibm-charts/ibm-websphere-liberty -f manifests/notification-slack-values.yaml -n notification-slack --namespace stock-trader --tls
```
