# mock-interceptor > $mock_version = 1.6.0

# [Changelog](https://github.com/gustafah/mock-interceptor/blob/main/CHANGELOG.md)

We've all been there, constantly waiting on APIs that never come, delaying our development.

To find an alternative, I created this library.

# How does it work?

Mock Interceptor works as an Interceptor to OkHttp. If mocks responses, as if they were coming straight from backends.
It searches for FILEs in the Assets folder, and use their content as the body of the response.
This gives you a lot of benefits, like:

- Offline development
- Infinite scenarios to test your feature
- Much more control over specific/corner cases
- and the best one: change between scenarios on the fly!

**NO BUILDING NEEDED**

# Setup

You can easily bring MockInterceptor to your project by adding this to your Module's **build.gradle**

```
implementation 'com.github.gustafah:mock-interceptor:$mock_version'
```

There is also a NoOp version of the library, to prevent unwanted code from going to versions not destined to testing, like release versions.
Here is an example on how to use it properly:

```
debugImplementation 'com.github.gustafah:mock-interceptor:$mock_version'
implementation 'com.github.gustafah:mock-interceptor-noclient:$mock_version'
```

After adding the dependency to the project, the first thing you need to do is Annotate your API class with @Mock annotation

***For the sake of this example, I'll assume the use of Retrofit***

```
interface SampleApi {
    @GET("posts")
    @Mock("posts.json")
    suspend fun fetch(): Response<List<FetchResponse>>

    @GET("posts")
    suspend fun fetchNoMock(): Response<List<FetchResponse>>

    @GET("posts")
    @Mock(files = ["posts.json", "error.json"])
    suspend fun fetchMultiMock(): Response<List<FetchResponse>>
}
```

As you can see above, MockInterceptor uses @Mock to stablish the name of the file(s) that it should look for in order to find the responses. But the lack of @Mock can also produce a mocked response, it's just a little tricker to name the file (we will talk about this later).

Now it's time to add MockInterceptor to your OkHttpClient.Builder

```
val client = OkHttpClient.Builder()
.addInterceptor(
    MockInterceptor.apply {
        config = MockConfig.Builder()
            .suffix(".json") //optional
            .separator("_") //optional
            .prefix("mock/") //optional
            .context { context } //mandatory
            .selectorMode(MockConfig.OptionsSelectorMode.STANDARD) //recommended
            .build()
    }
)
.build()
```

Using **MockConfig.Builder**, we add all the information it needs to find and process our files.
- **suffix, prefix and separator**:  are optionals, and are used to "guess" the name of the files (when you don't use @Mock, will explain later)
- **context**: a function to provide an always active context to the MockInterceptor. This is required for displaying the Dialog.
- **selectorMode**: How the MockInterceptor will behave:
  - STANDARD: displays a Dialog on the screen. This, sometimes, can cause the Dialog to show under certain views in your layout.
  - ALWAYS_ON_TOP: opens an empty Activity, and displays the Dialog inside it. This ensures that the Dialog will always display on top of all the layout, but may cause a loop if your Request was called inside of **onResume**
  - NO_SELECTION: Won't display a dialog, and will always use the **default** option, when default >= 0, or the first mock option, when default < 0. (will be explained next section)

Then, all it's left to do is add this client to your Retrofit.Builder

# How to create mock files?

Mock files have to be placed inside the assets folder. For now, MockInterceptor only accepts JSON formatted files. Let's see how to create them:

## The basic structure

The basic structure of the Mock File has the following parameters:
```
{
  "reference": "Mock Name",
  "default": -1,
  "saved_data": []
}
```

- **reference**: a string that defines a Title for this entire Mock
- **default**: a integer that can provide a default option to be selected everytime. It can be:
  - -2: for random responses (no dialog will be presented)
  - -1: for no default response (dialog will be presented, unless there is only 1 option)
  - 0..N: to select the Nth position inside the __saved_data__ array (no dialog will be presented)
- **saved_data**: the array that contains the actual mocked options

The json structure for each mocked option (inside saved_data) has the following parameters:
```
{
  "description": "Title",
  "code": 200,
  "data_array|data_path|data|is_unit": ...
}
```

- **description**: a string that defines the Title for this option
- **code**: represents the REST code that will be returned from the "api call"
- the actual content, that will be the ```body``` of this option ca be described in different ways:
  - **data_path**: a string, stating that the response for this option is, actually, inside another asset file (this enhances organization and a shorter mock file)
  - **data_array**: a jsonarray, stating that the response for this option will be an Array (makes sense to the parser)
  - **data**: a jsonobject, stating that the response for this option will be an Object (make sense to the parser)
  - **is_unit**: a boolean, stating that the response fot this option doesn't have a body

Now we know everything we need to know to start creating our first Mock

# Example file

Let's see an example that ilustrates what we've learned so far

```
{
  "reference": "List of Users",
  "default": -1,
  "saved_data": [
    {
      "description": "Get from file",
      "code": 200,
      "data_path": "users/list.json"
    },
    {
      "description": "Array",
      "code": 200,
      "data_array": [
        {
          "id": 1,
          "name": "Gustafah"
        },
        {
          "id": 2,
          "name": "Github"
        },
        {
          "id": 3,
          "name": "Master"
        }
      ]
    },
    {
      "description": "No Content - Error?",
      "code": 400,
      "is_unit": true
    },
    {
      "description": "Error",
      "code": 500,
      "data": {
        "message": "Unexpected error while getting the list",
        "type": "UNEXPECTED_ERROR"
      }
    }
  ]
}
```

Just by glimpsing at this example, we can already tell that it's very easy for a mock file to become a big mess. That's why we strongly adcive on using **data_path** for adding Mock files, they allow you to have a propper organization in your assets folder and your mock file.

# Non @Mock mocks

As I've said before, the use for @Mock is optional (but strongly advised). You can still have mock files without it, let's see how.

MockInterceptor has the functionality of "guessing" the name of the mock it should be used when @Mock is not found inside the request. It does so by using the **path**, **parameters**, **method** and the options provided in the MockConfig.Builder. Let's see an example.

```
//MockInterceptor setup
config = MockConfig.Builder()
  .suffix(".json")
  .separator("_")
  .prefix("mock/")
  .context { context } //mandatory
  .build()
  
//API Request
@GET("app/user/posts")
suspend fun fetchNoMock(): Response<List<FetchResponse>>
```

When MockInterceptor get's this request, it will notice the absense of @Mock, and will proceed in trying to guess the name of the file in the following structure:

> {prefix}app{separator}user{separator}posts{separator}{method}{sufix}

Resulting in

> "mock/app_user_posts_GET.json"

so the file ```app_user_posts_GET.json``` should be created in your ```assets\mock``` folder.

Whenever a file is not found, either by a wrongly named @Mock or by an incorrect "guess", it will return an error response, with the following structure:

```
{
  "type": "SERVICE_UNAVAILABLE",
  "message": "Couldn't find a mock for this request. (suggestion: mock/app_user_posts_GET.json)"
}
```

# Record and Playback Mocks

With this library you can also record API responses on a database and play it back if you want to map your API easily. 

## Record Mocks

To start recording your mock files, you need to just configure your Mock Interceptor as such:
```
//MockInterceptor setup
config = MockConfig.Builder()
  .saveMockMode(OptionRecordMock.RECORD)
  .context { context } //mandatory
  .build()
```

After this configuration, all the API calls will be stored in the database. If a duplicated call occurs, will be displayed a dialog asking if you want to replace the information or ignore. You can also set this on default on the MockInterceptor initialization. 

```
//MockInterceptor setup
config = MockConfig.Builder()
  .saveMockMode(OptionRecordMock.RECORD)
  .replaceMockOption(ReplaceMockOption.DEFAULT | KEEP_MOCK | REPLACE_MOCK)
  .context { context } //mandatory
  .build()
```

You can also save the mock under some identification (for example, you have multiple users and you need to keep their mocks apart). You only need on run time, define witch identification you want to save the mocks.

```
MockInterceptor.setMockGroupIdentifier("The name of the group")
```

## Playback Mocks

After saving all the mocks, you can playback them. You just need to change on the Mock Interceptor setup

```
//MockInterceptor setup
config = MockConfig.Builder()
  .saveMockMode(OptionRecordMock.PLAYBACK)
  .context { context } //mandatory
  .build()
```

and all api calls will be redirected to check if there is the information saved on the database. If you saved under some indentification, you just need to set the group identifier that you record. You can also get the list of group identifiers stored on the database with the function:

> fun getAllMockIdentifiers() : List<String>

## Export, Import, Replace and Delete Data

You can control all the information saved on your database with those commands:

**Export Database**
The function will ask for READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE from de device, and export all data saved as a .db or a .zip containing all jsons stored.
> fun exportDatabase()

**Import Database**
The function will ask for READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE from de device, and import a Json file, a .db file or a .zip file containing all .jsons files you want to import.
> fun importDatabase()

**Replace Database**
The function will delete the current database and replace from the content of a .db file.
> fun replaceDatabase()

**Delete Database**
The function will delete the current database.
> fun deleteDatabase()

You can also observe the database operation state with the MockInterceptor liveData. You just need to watch like:

```
MockInterceptor.databaseObserver.observe(this) { state ->
    when (state) {
        MockDatabaseState.LOADING -> TODO()
        MockDatabaseState.SUCCESS -> TODO()
        MockDatabaseState.ERROR -> TODO()
    }
}
```

That's all for now

# Thanks
