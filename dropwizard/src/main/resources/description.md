## Web console

To monitor and manage your processes workflow every taskurotta server has an embedded web
console. It can be open in a web browser and provides friendly UI for accessing cluster data.

### Features

  - UI is web based - accessible via browser
  - Every cluster's node console provide the same cluster wide information
  - REST based - data can be extracted directly as JSON objects (simplifies integration with 3d party systems)
  - Provides possibility to register and launch tasks on schedule
  - Displays information on current state of the actor's queues
  - Displays information on actors activity
  - Displays current workflows data: tasks, arguments, properties and return values

Console UI consists of a set of tabbed views: *"Queues"*, *"Actors"*, *"Processes"*, *"Monitoring"*, *"Schedule"*. Each of
them is described below in details.

### Queues view

  Provides a paginated list of all actors's task queues registered in a cluster. It is important to know that the information presented
  can be delayed by a few seconds from the actual one. Console uses Taskurotta's metrics service as a source for the information, and it
  requires some time for the data to be updated. Also, all metrics data for the node is lost when is JVM shutting down or reloads.

  Displayed fields are:

  - *"Name"* - task queue name (Note that hazelcast backing collection prefix is omitted). It generally matches the actor ID bind to the queue.
  - *"Last polled"* - last date and time (on the server) when an actor polled task from the queue. It could be any actor polled any node.
  - *"Balance"* - estimate data on task income/outcome of the queue. Income is a summed up number of enqueue invocations for all nodes,
  outcome is a summed up number of task polled from the queue. You can switch between last hour and last 24hours
  - *"Size"* - size of the queue by metrics data. By clicking eye-button ("Show current size") you can get an actual current size by requesting
  the queue itself. Due to performance issues that value cannot be auto updated.

  Note that:

  - It is possible to use *name filter* for filtering queues list. In that case, only queues with names starting with filter value would remain.
  - Pagination and auto refresh properties are stored as a browser cookies. It enables view to keep its state unchanged on browser page
  refresh (and it can be reset by deleting cookies *"queue.list.pagination"* and *"queue.controller.selection"*).

  By clicking queue name link you would be navigated to the list of task UUIDs currently contained at the queue. For large queues this operation
  can take significant amount of time and is not recommended to use.

### Actors view

  Provides list of all actors registered in a cluster. It also enables you to block or unblock actors and compare metrics data for them.
  Displayed fields are:

  - *"Actor ID"* - ID of a registered actor
  - *"Queue balance"* - shows queue task income and outcome similar to Queues view. It also estimates rate of task income as:
   <div style="margin:10px;padding:10px; border: 1px dotted #777;">
       [number of polled task]/[last - first poll time for period] - [number of new enqueued tasks]/[last - first enqueue time for period]
   </div>
   Red arrow in general means that queue is overflowing with task, green arrow means that queue is OK.
  - *"Last activity"* - last date and time (on the server) when an actor of this type polled for task and released task decision. It could be any actor on any node.
  - *"Actions"* - there are two user actions available at the moment: actor *blocking/unblocking* and performance *comparison by metrics* data. When actor is blocked,
  it would get *null* on every poll request, as if it has no task in the queue. By checking two or more actors and clicking "Compare" link actor comparison view is
  showed. There are two main areas on the view: available metrics checkbox list and comparison table. Every metrics checkbox corresponds to the table column. Two
  metrics are checked by default, it is successfulPoll and enqueue metrics. You are free to check as many metrics as you need.

  Note: actor IDs selected for comparison are stored as browser cookie with name *"actors.compare.actorIds"*. So actors stay selected on browser page reload.

### Processes view

  Processes view is designed to provide data on workflows splitted into separate subviews.

  #### List views

  #### Search views

  #### Create process view

  #### Broken processes views


### Monitoring view


### Schedule view

