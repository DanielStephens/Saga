# Saga
This is small project looking to implement the saga pattern in java.

## What is the saga pattern?
For a more indepth explanation see this link: https://microservices.io/patterns/data/saga.html
The TLDR is that the saga pattern is one way of keeping business data in a consistent state when working with microservices. It fulfills a similar role to transactions but in a distributed architecture.
Given you need to perform some business action by invoking [action_service]
Given you need to audit that action by invoking [audit_service]
Given that the data model is only consistent if we either do BOTH of the above, or NEITHER.
A saga can help in this situation by performing the first action an waiting until it is known to be successful or not, then proceeding to do the next step (auditting). If any step goes wrong we can perform some compensation task, for example to retry our current action or undo previous actions.

## Where do I start?
This project is in the early stages and isn't set up to be particularly easy to look through, I've added and commented an integration test which shows some of what the library does so check that out for now! [IntegrationTest](core/src/test/java/IntegrationTest.java)
