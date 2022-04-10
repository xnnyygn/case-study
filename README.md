# case-study

## An Approach for Ticket Reservation Sites

Package: `in.xnnyygn.concurrent.reservation`

Blog(Japanese): [https://tech.unifa-e.com/entry/2021/08/28/063551](https://tech.unifa-e.com/entry/2021/08/28/063551)

![reservation](https://github.com/xnnyygn/case-study/blob/main/src/main/resources/reservation.png?raw=true)

The basic idea is to add all the incoming requests to a concurrent queue, which might be an ordinary database table or
a FIFO queue provided by Amazon Web Service, then a single thread is responsible for granting the permit to the
user in the head of the queue. If a user gets the permit, the user will be notified and he/she can start to use
the site to reserve tickets. After the user finishes reservation, the permit he/she used will be returned to a
temporary bucket and the single thread will collect permits from the bucket periodically.

This approach allows you to limit the count of online users so that your site won't be down because of too many users.
You could still use a simple database table design without thinking of the heavy access of the users and fewer users
mean you don't need to add some pessimistic locks to make sure the reservation is processed correctly.

However, this approach assumes the user does not use the site for a long time, so it might be a problem if a user
doesn't quit your site. The user will keep the permit forever and other users cannot reserve tickets. A simple solution
is to add timeouts to those permits. The single thread will check the granted permits periodically and cancel the
permits which have been used for a long time. You need to use compare-and-swap to change the state of the permit because
a user might just finish reservation and attempt to return the permit.

## A Parallel Executor for Dependent Resources with Transaction-like Rollback Feature

Package: `in.xnnyygn.concurrent.dependency`

Blog(Japanese): [https://tech.unifa-e.com/entry/2021/11/02/100646](https://tech.unifa-e.com/entry/2021/11/02/100646)

![dependency](https://github.com/xnnyygn/case-study/blob/main/src/main/resources/dependency.png?raw=true)

Most modern programming languages come with some official tools to download dependencies parallel and this executor is
just like those tools. It analyses the dependency and start executing from those independent resources. If there is a
cycle, the executor will complain and then stop. It's just some graph related algorithms we learnt at university. To
make sure we gain the most performance the executor makes use of fine-grained locks, for example, if resource A is
depending on resource B and C, resource A will be notified only by resource B and C, not some resource D.

However, this executor has a special feature to rollback all the executions like a transaction because it is intended
to implement some cool features like AWS CloudFormation. Parallel executors for dependent resources are also used in
infra tools like Terraform and AWS CloudFormation. I think transaction is an important feature for these tools
since there certainly will be some problem if the resource creation or update is failed during the execution. To
reverse the execution is just like to start a new parallel executor(actually it is) with reversed dependency graph.
You can find more information in the code itself.

## Transactional Memory (TL2)

Package: `in.xnnyygn.concurrent.transaction`

## Fuzzy Search ##

Package: `in.xnnyygn.concurrent.fuzzysearch`

Basic version of Levenshtein Automaton. The idea is to create a statemachine for the input source then try to apply 
the prebuilt trie to the statemachine and to find words within a specified levenshtein edit distance.