# MultiLoader
AsyncTaskLoader Callbacks "injection" library for Android which uses annotation processing to generate boilerplate code. It focuses on using multiple AsyncTaskLoaders in a Activity or a Fragment.


- Eliminate anonymous inner-classes for callbacks by annotating methods with `@OnCreateLoader`, `@OnLoadFinished` and others.

```java
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MultiLoader.inject(this, LOADER_ID_1, LOADER_ID_2);
}

@OnCreateLoader(LOADER_ID_1)
public Loader<Data> onCreateDataLoader(int id, Bundle args) { ... }

@OnLoadFinished(LOADER_ID_1)
public void onDataLoadFinished(Loader<Data> loader, Data data) { ... }

@OnCreateLoader(LOADER_ID_2)
public Loader<MessageList> onCreateMessageListLoader(int id, Bundle args) { ... }

@OnLoadFinished(LOADER_ID_2)
public void onMessageListLoadFinished(Loader<MessageList> loader, MessageList data) { ... }
```
