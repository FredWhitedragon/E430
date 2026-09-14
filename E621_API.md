# E621 API

> 来源：用户提供的 `Help_ API - e621.html`（[原页面](https://e621.net/help/api)）。本文按上传文件整理，保留英文技术说明、参数名、返回字段及示例，移除网页导航并展开折叠内容；未联网核验或补充外部文档。
> 原页面说明：下列接口文档并非持续维护，可能不准确或过时，较新的文档请参阅文内 OpenAPI 链接。原始示例中的占位符、拼写及 JSON 格式问题按原文保留，不保证可直接执行；认证示例中的 API key 为原文标注的虚构示例。

## Database export <span id="dbexport"></span>

If you are looking for bulk data, or are expecting to do tens of thousands of lookups, please use the daily data exports available at <https://e621.net/db_export/>

## API <span id="api"></span>

e621 offers a simple API to make scripting easy. All you need is a way to send GET, POST, PATCH and DELETE calls to URLs.  
The ability to parse JSON is nice, but not critical. The simplicity of the API means you can write scripts using JavaScript, Perl, Python, Ruby, and even shell languages like bash or tcsh.

<span id="basics"></span>

## Basic Concepts

HTTP defines four request methods: GET, POST, PATCH and DELETE.  
You will be using these four methods to interact with the e621 API.

Most API calls that create a new database entry use POST, calls that update an existing entry use PATCH, and calls that only retrieve data use GET.

In the e621 API, a URL is analogous to a function name. You pass in the function parameters as a query string.  
Here is an extremely simple example that will return 10 most recent posts:  
<https://e621.net/posts.json?limit=10>

HTTP POST request bodies should be urlencoded, or use multipart form data content encoding, and you should set the content-type header to the appropriate value for the encoding used.  
Urlencoded POST bodies cannot contain files, you must use multipart form data encoding if you are uploading a file.  
Almost all HTTP request libraries will included routines to do all of this encoding automatically for you, and you SHOULD use them.

### User Agents

A non-empty User-Agent header is required for all requests. Please pick a descriptive User-Agent for your project.  
You should include your e621 username, so that you may be contacted if your project causes problems.  
For example: `MyProject/1.0 (by username on e621)`

> **Important!**
>
> Do not impersonate a browser user agent, as this will get you blocked.

Due to frequent abuse, default user agents for programming languages and libraries are usually blocked.  
Please make sure that you are defining a user agent that is in line with our policy.

In some cases, you may be unable to set a custom header for your requests.  
This may be because you are creating a userscript, a browser extension, or otherwise something that works within a browser.  
If that is the case, please attach an additional url quest parameter named `_client`, and set it to your user-agent.

### Authorization

The API requires that API access be enabled on the account before it can use the API.  
To enable API access, you must go to your profile page (Account \> My profile) and generate an API key.

All actions that handle private user data require authorization.  
The preferred method is by sending Basic auth in the `Authorization` header.

**JS Example**

    const username = "hexerade";
    const apiKey = "1nHrmzmsvJf26EhU1F7CjnjC"; // Not an actual API key lol
    const response = await fetch("https://e621.net/posts.json", {
      headers: {
        "Authorization": "Basic " + btoa(`${username}:${apiKey}`),
        "User-Agent": "MyProject/1.0 (by username on e621)"
      }
    });
    console.log(await response.json());

You can alternatively send the `login` and `api_key` parameters as part of the query string if you aren't able to set headers.  
For example: `https://e621.net/posts.json?login=hexerade&api_key=1nHrmzmsvJf26EhU1F7CjnjC`

### Rate Limiting

E621/E926 have a hard rate limit of two requests per second.  
This is a hard upper limit, and if you are hitting it, you are already going way too fast.  
Hitting the rate limit will result in a 503 HTTP response code.

> **Important!**
>
> You should make a best effort not to make more than one request per second over a sustained period.

### CORS

The API allows simple requests as defined in the [MDN Web Docs](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#simple_requests).  
What this means for you is that `GET` and `POST` requests are possible cross-origin, while `PATCH`, `PUT` and `DELETE` are not.

### Responses

All API calls that change state will return a single element response. They are formatted like this: `{success: false, reason: "duplicate"}`

While you can usually determine success or failure based on the response object, you can also figure out what happened based on the HTTP status code.  
In addition to the standard ones, e621 uses some custom status codes in the 4xx and 5xx range.

| Status Code                    | Meaning                                                                                                                                                  |
|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| 200 OK                         | Request was successful                                                                                                                                   |
| 204 No Content                 | Request was successful, nothing will be returned. Most often encountered when deleting a record.                                                         |
| 400 Bad Request                | The requested feature (such as an IQDB query) is not available.                                                                                          |
| 401 Unauthorized               | Your authorization is invalid. Ensure your username and api key are correct.                                                                             |
| 403 Forbidden                  | Access denied. May indicate that your request lacks a User-Agent header (see Notice \#2 above)                                                           |
| 404 Not Found                  | Not found                                                                                                                                                |
| 405 Method Not Allowed         | The request method you used is not allowed for the URL. Typically when you encounter this, switching between POST/PATCH/PUT will fix the issue.          |
| 406 Not Acceptable             | The requested format is not allowed. Everything except for the posts index only responds to html and json. Some routes only respond to one or the other. |
| 410 Gone                       | The requested resources is gone. This will typically be encountered when invalid pagination is attempted.                                                |
| 412 Precondition failed        | Some condition failed. Typically, this means your upload is invalid or a duplicate.                                                                      |
| 422 Unprocessable Content      | Something in your request body was invalid. Typically, this means you either forgot a parameter, or its format was invalid.                              |
| 429 Ratelimited                | You are sending requests too fast. Back off for a little bit, and slow your rate of requests.                                                            |
| 500 Internal Server Error      | Some unknown error occurred within e621.                                                                                                                 |
| 502 Bad Gateway                | An issue happened when attempting to reach e621. Try again later.                                                                                        |
| 503 Service Unavailable        | Server cannot currently handle the request or you have exceeded the request rate limit. Try again later or decrease your rate of requests.               |
| 520 Unknown Error              | Unexpected server response which violates protocol                                                                                                       |
| 522 Origin Connection Time-out | CloudFlare's attempt to connect to the e621 servers timed out.                                                                                           |
| 524 Origin Connection Time-out | A connection was established between CloudFlare and the e621 servers, but it timed out before an HTTP response was received.                             |
| 525 SSL Handshake Failed       | The SSL handshake between CloudFlare and the e621 servers failed.                                                                                        |

## OpenAPI Specification <span id="spec"></span>

There is a community maintained OpenAPI specification for the api located at [https://e621.wiki](https://e621.wiki/).  
If you would like to contribute or report an issue, you can find more information in [topic \#46279](https://e621.net/forum_topics/46279).  
A full url to the spec file for use in IDEs and such:

> <https://e621.wiki/openapi.yaml>

Note that the endpoint list below may be incorrect or out of date as it is not actively maintained. For up to date documentation, see [The OpenAPI Specification](#spec).

## Posts <span id="posts"></span>

### <span id="posts_create"></span>Create

The base URL is `/uploads.json` called with POST.  
There are only four mandatory fields: you need to supply the file (either through a multipart form or through a source URL), the tags, a source (even if blank), and the rating.

- `upload[tag_string]` A space delimited list of tags.
- `upload[file]` The file data encoded as a multipart form.
- `upload[rating]` The rating for the post. Can be: `s`, `q` or `e` for safe, questionable, and explicit respectively.
- `upload[direct_url]` If this is a URL, e621 will download the file.
- `upload[source]` This will be used as the post's 'Source' text. Separate multiple URLs with %0A (url-encoded newline) to define multiple sources. Limit of ten URLs
- `upload[description]` The description for the post.
- `upload[parent_id]` The ID of the parent post.
- `upload[as_pending]`

If the call fails, the following response reasons are possible:

- `MD5` mismatch This means you supplied an MD5 parameter and what e621 got doesn't match. Try uploading the file again.
- `duplicate` This post already exists in e621 (based on the MD5 hash). An additional attribute called location will be set, pointing to the (relative) URL of the original post.
- `other` Any other error will have its error message printed.

**Response:**

Success:  
HTTP 200 OK

    {
        "success":true",
        ”location":"/posts/<Post_ID>",
        "post_id":<Post_ID>
    }

Failed due to the post already existing:  
HTTP 412

    {
        "success":false,
        "reason":"duplicate",
        "location":"/posts/<Post_ID>",
        "post_id":<Post_ID>
    }

### <span id="posts_update"></span>Update

The base URL is `/posts/<Post_ID>.json` called with PATCH.  
Leave parameters blank if you don't want to change them.

- `post[tag_string_diff]` A space delimited list of tag changes such as `dog -cat`. This is a much preferred method over the old version.

(The old method of updating a post's tags still works, with `post[old_tag_string]` and `post[tag_string]`, but `post[tag_string_diff]` is preferred.)

- `post[source_diff]` A (URL encoded) newline delimited list of source changes. This works the same as `post[tag_string_diff]` but with sources.

(The old method of updating a post's sources still works, with `post[old_source]` and `post[source]`, but `post[source_diff]` is preferred.)

- `post[parent_id]` The ID of the parent post.
- `post[old_parent_id]` The ID of the previously parented post.
- `post[description]` This will be used as the post's 'Description' text.
- `post[old_description]` Should include the same descriptions submitted to `post[description]` minus any intended changes.
- `post[rating]` The rating for the post. Can be: `s`, `q` or `e` for safe, questionable, and explicit respectively.
- `post[old_rating]` The previous post's rating.
- `post[is_rating_locked]` Set to true to prevent others from changing the rating.
- `post[is_note_locked]` Set to true to prevent others from adding notes.
- `post[edit_reason]` The reason for the submitted changes. Inline DText allowed.

### <span id="posts_list"></span>List

\[ [Example JSON output](https://e621.net/posts.json?tags=fluffy+rating:s&limit=5) \]

The base URL is `/posts.json` called with GET.  
Deleted posts are returned when `status:deleted`/`status:any` is in the searched tags.

The most efficient method to iterate a large number of posts is to search use the `page` parameter, using `page=b<ID>` and using the lowest ID retrieved from the previous list of posts. The first request should be made without the page parameter, as this returns the latest posts first, so you can then iterate using the lowest ID. Providing arbitrarily large values to obtain the most recent posts is not portable and may break in the future.

Note: Using `page=<number>` without `a` or `b` before the number just searches through pages. Posts will shift between pages if posts are deleted or created to the site between requests and page numbers greater than 750 will return an error.

- `limit` How many posts you want to retrieve. There is a hard limit of 320 posts per request. Defaults to the value set in user preferences.
- `tags` The tag search query. Any tag combination that works on the website will work here.
- `page` The page that will be returned. Can also be used with `a` or `b` + post_id to get the posts after or before the specified post ID. For example `a13` gets every post after post_id 13 up to the limit. This overrides any ordering meta-tag, `order:id_desc` is always used instead.

This returns a JSON array, for each post it returns:

- `id` The ID number of the post.

- `created_at` The time the post was created in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.

- `updated_at` The time the post was last updated in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.

- `file` (array group)

- - `width` The width of the post.
  - `height` The height of the post.
  - `ext` The file's extension.
  - `size` The size of the file in bytes.
  - `md5` The md5 of the file.
  - `url` The URL where the file is hosted on E6

- `preview` (array group)

- - `width` The width of the post preview.
  - `height` The height of the post preview.
  - `url` The URL where the preview file is hosted on E6

- `sample` (array group)

- - `has` If the post has a sample/thumbnail or not. (True/False)
  - `width` The width of the post sample.
  - `height` The height of the post sample.
  - `url` The URL where the sample file is hosted on E6.

- `score` (array group)

- - `up` The number of times voted up.
  - `down` A negative number representing the number of times voted down.
  - `total` The total score (up + down).

- `tags` (array group)

- - `general` A JSON array of all the `general` tags on the post.
  - `copyright` A JSON array of all the `copyright` tags on the post.
  - `contributor` A JSON array of all the `contributor` tags on the post.
  - `species` A JSON array of all the `species` tags on the post.
  - `character` A JSON array of all the `character` tags on the post.
  - `artist` A JSON array of all the `artist` tags on the post.
  - `invalid` A JSON array of all the `invalid` tags on the post.
  - `lore` A JSON array of all the `lore` tags on the post.
  - `meta` A JSON array of all the `meta` tags on the post.

- `locked_tags` A JSON array of tags that are locked on the post.

- `change_seq` An ID that increases for every post alteration on E6 (explained below)

- `flags` (array group)

- - `pending` If the post is pending approval. (True/False)
  - `flagged` If the post is flagged for deletion. (True/False)
  - `note_locked` If the post has it's notes locked. (True/False)
  - `status_locked` If the post's status has been locked. (True/False)
  - `rating_locked` If the post's rating has been locked. (True/False)
  - `deleted` If the post has been deleted. (True/False)

- `rating` The post's rating. Either `s`, `q` or `e`.

- `fav_count` How many people have favorited the post.

- `sources` The source field of the post.

- `pools` An array of Pool IDs that the post is a part of.

- `relationships` (array group)

- - `parent_id` The ID of the post's parent, if it has one.
  - `has_children` If the post has child posts (True/False)
  - `has_active_children`
  - `children` A list of child post IDs that are linked to the post, if it has any.

- `approver_id` The ID of the user that approved the post, if available.

- `uploader_id` The ID of the user that uploaded the post.

- `description` The post's description.

- `comment_count` The count of comments on the post.

- `is_favorited` If provided auth credentials, will return if the authenticated user has favorited the post or not.

change_seq is a number that is increased every time a post is changed on the site. It gets updated whenever a post has any of these values change:

    tag_string
    source
    description
    rating
    md5
    parent_id
    approver_id
    is_deleted
    is_pending
    is_flagged
    is_rating_locked
    is_pending
    is_flagged
    is_rating_locked

### <span id="posts_flag"></span>Flag

  
[Listing](#flags_listing) \| [Creating](#flags_creating)

> #### <span id="flags_listing"></span>Listing
>
> The base URL is `/post_flags.json` called with GET.
>
> - `search[post_id]` The ID of the flagged post.
> - `search[creator_id]` The user's ID that created the flag.
> - `search[creator_name]` The user's name that created the flag.
>
> Returns:
>
> - `id` The flag's ID
> - `created_at` The time the flag was created in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.
>
> `post_id` The ID of the post that the flag applies to.  
> `reason` The reason submitted for the flag.  
> `is_resolved` If the flag has been handled (True/False)  
> `updated_at` The time the flag was last updated in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.
>
> - `is_deletion` If the flag is a flag for deletion.
> - `category` The category of the flag.

> #### <span id="flags_creating"></span>Creating
>
> The base URL is `/post_flags.json` and called with POST. Both fields are required.
>
> - `post_flag[post_id]` The ID of the flagged post.
> - `post_flag[reason_name]` The reason submitted along with the flag, eg. "inferior".
> - `post_flag[parent_id]` ID of the superior post when flagging an image as inferior.
>
> If successful it will return the added note in the same format as the Listing section above.

### <span id="posts_vote"></span>Vote

  
The base URL is `/posts/<Post_ID>/votes.json` called with POST.

- `score` Set to 1 to vote up and -1 to vote down. Repeat the request to remove the vote.
- `no_unvote` Set to true to have this score replace the old score. Repeat votes will not remove the vote.

**Response:**

Success:  
HTTP 200

    {
       "score":<total>,
       "up":<up>,
       "down":<down>,
       "our_score":x
    }

Where our_score is 1, 0, -1 depending on the action.  
Failure:  
HTTP 422

    {
        "success": false,
        "message": "An unexpected error occurred.",
        "code": null
    }

## Favorites <span id="favorites"></span>

### <span id="favorites_listing"></span>Listing

The base URL is `/favorites.json` called with GET.

- `user_id` Optional, the user to fetch the favorites from. If not specified will fetch the favorites from the currently authorized user.

**Response:**

Success:  
HTTP 200

See [\#posts_list](#posts_list) for post data specification.

    {
        "posts": [
            <post data>
        ]
    }

Error:  
HTTP 403 if the user has hidden their favorites.  
HTTP 404 if the specified `user_id` does not exist or `user_id` is not specified and the user is not authorized.

### <span id="favorites_create"></span>Create

The base URL is `/favorites.json` called with POST.

- `post_id` The post id you want to favorite.

**Response:**

Success:  
HTTP 200

See [\#posts_list](#posts_list) for post data specification.

    {
       "post": <post data>
    }

### <span id="favorites_delete"></span>Delete

The base URL is `/favorites/<post_id>.json` called with DELETE.

There is no response.

## Tags <span id="tags"></span>

[Listing](#tags_listing)

### <span id="tags_listing"></span>Listing

The base URL is `/tags.json` called with GET.

- `search[name_matches]` A tag name expression to match against, which can include `*` as a wildcard.
- `search[category]` Filters results to a particular category. Default value is blank (show all tags). See below for allowed values.
- `search[order]` Changes the sort order. Pass one of `date` (default), `count`, or `name`.
- `search[hide_empty]` Hide tags with zero visible posts. Pass `true` (default) or `false`.
- `search[has_wiki]` Show only tags with, or without, a [wiki page](https://e621.net/wiki_pages). Pass `true`, `false`, or blank (default).
- `search[has_artist]` Show only tags with, or without an [artist page](https://e621.net/artists). Pass `true`, `false`, or blank (default).
- `limit` Maximum number of results to return per query. Default is 75. There is a hard upper limit of 320.
- `page` The page that will be returned. Can also be used with a or b + tag_id to get the tags after or before the specified tag ID. For example a13 gets every tag after tag_id 13 up to the limit. This overrides the specified search ordering, `date` is always used instead.

**Categories:**

The following values can be specified.

- `0` general
- `1` artist
- `2` contributor
- `3` copyright
- `4` character
- `5` species
- `6` invalid
- `7` meta
- `8` lore

[See here](https://e621.net/wiki_pages/11262) for a description of what different types of tags are and do.

**Response:**

Success:  
HTTP 200

    [{
       "id":<numeric tag id>,
       "name":<tag display name>,
       "post_count":<# matching visible posts>,
       "related_tags":<space-delimited list of tags>,
       "related_tags_updated_at":<ISO8601 timestamp>,
       "category":<numeric category id>,
       "is_locked":<boolean>,
       "created_at":<ISO8601 timestamp>,
       "updated_at":<ISO8601 timestamp>
    },
    ...
    ]

If your query succeeds but produces no results, you will receive instead the following special value:

    { "tags":[] }

## Tag Aliases <span id="tag_aliases"></span>

[Listing](#tag_alias_listing)

Note: Everything here also applies to Tag Implications, you just need to swap out `tag_aliases` with `tag_implications` when constructing URLs.

### <span id="tag_alias_listing"></span>Listing

The base URL is `/tag_aliases.json` called with GET.

- `search[name_matches]` A tag name expression to match against, which can include `*` as a wildcard. Both the aliased-to and the aliased-by tag are matched.
- `search[antecedent_name]` Supports multiple tag names, comma-separated.
- `search[consequent_name]` Supports multiple tag names, comma-separated.
- `search[antecedent_tag_category]` Pass a valid tag category. Supports multiple values, comma-separated.
- `search[consequent_tag_category]` Pass a valid tag category. Supports multiple values, comma-separated.
- `search[creator_name]` Name of the creator.
- `search[approver_name]` Name of the approver.
- `search[status]` Filters aliases by status. Pass one of `approved`, `active`, `pending`, `deleted`, `retired`, `processing`, `queued`, or blank (default). \*
- `search[order]` Changes the sort order. Pass one of `status` (default), `created_at`, `updated_at`, `name`, or `tag_count`.
- `limit` Maximum number of results to return per query.
- `page` The page that will be returned. Can also be used with a or b + alias_id to get the aliases after or before the specified alias ID. For example a13 gets every alias after alias_id 13 up to the limit. This overrides the specified search ordering, `created_at` is always used instead.

\* Some aliases have a status which is an error message, these show up in searches where status is omitted but there is no way to search for them specifically.

**Response:**

Success:  
HTTP 200

    [{
       "id": <numeric alias id>,
       "status": <status string>,
       "antecedent_name": <aliased-by tag name>,
       "consequent_name": <aliased-to tag name>,
       "post_count": <# matching posts>,
       "reason": <explanation>,
       "creator_id": <user id>,
       "approver_id": <user id>
       "created_at": <ISO8601 timestamp>,
       "updated_at": <ISO8601 timestamp>,
       "forum_post_id": <post id>,
       "forum_topic_id": <topic id>,
    },
    ...
    ]

If your query succeeds but produces no results, you will receive instead the following special value:

    { "tag_aliases":[] }

## Notes <span id="notes"></span>

### <span id="notes_listing"></span>Listing

The base URL is `/notes.json` called with GET.

- `search[body_matches]` The note's body matches the given terms. Use a \* in the search terms to search for raw strings.
- `search[post_id]`
- `search[post_tags_match]` The note's post's tags match the given terms. Meta-tags are not supported.
- `search[creator_name]` The creator's name. Exact match.
- `search[creator_id]` The creator's user id.
- `search[is_active]` Can be: true, false
- `limit` Limits the amount of notes returned to the number specified.

This returns a JSON array, for each note it returns:

- `id` The Note's ID
- `created_at` The time the note was created in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.
- `updated_at` The time the mote was last updated in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.
- `creator_id` The ID of the user that created the note.
- `x` The X coordinate of the top left corner of the note in pixels from the top left of the post.
- `y` The Y coordinate of the top left corner of the note in pixels from the top left of the post.
- `width` The width of the box for the note.
- `height` The height of the box for the note.
- `version` How many times the note has been edited.
- `is_active` If the note is currently active. (True/False)
- `post_id` The ID of the post that the note is on.
- `body` The contents of the note.
- `creator_name` The name of the user that created the note.

If no results are returned:

    {"notes":[]}

### <span id="notes_create"></span>Create

The base URL is `/notes.json` called with POST.

- `note[post_id]` The ID of the post you want to add a note to.
- `note[x]` The X coordinate of the top left corner of the note in pixels from the top left of the post.
- `note[y]` The Y coordinate of the top left corner of the note in pixels from the top left of the post.
- `note[width]` The width of the box for the note.
- `note[height]` The height of the box for the note.
- `note[body]` The contents of the note.

All fields are required.

If successful it will return the added note in the same format as the Listing section above.

### <span id="notes_update"></span>Update

The base URL is `/notes/<Note_ID>.json` called with PUT.

- `note[x]` The X coordinate of the top left corner of the note in pixels from the top left of the post.
- `note[y]` The Y coordinate of the top left corner of the note in pixels from the top left of the post.
- `note[width]` The width of the box for the note.
- `note[height]` The height of the box for the note.
- `note[body]` The contents of the note.

All fields are required.

If successful it will return the added note in the same format as the Listing section above.

### <span id="notes_delete"></span>Delete

The base URL is `/notes/<Note_ID>.json` called with DELETE.

There is no response.

### <span id="notes_revert"></span>Revert

The base URL is `/notes/<Note_ID>/revert.json` called with PUT.

- `version_id` The note version id to revert to.

## Pools <span id="pools"></span>

### <span id="pools_listing"></span>Listing

The base URL is `/pools.json` called with GET.

- `search[name_matches]` Search pool names.
- `search[id]` Search for a pool ID, you can search for multiple IDs at once, separated by commas.
- `search[description_matches]` Search pool descriptions.
- `search[creator_name]` Search for pools based on creator name.
- `search[creator_id]` Search for pools based on creator ID.
- `search[is_active]` If the pool is active or hidden. (True/False)
- `search[category]` Can either be "series" or "collection".
- `search[order]` The order that pools should be returned, can be any of: `name`, `created_at`, `updated_at`, `post_count`. If not specified it orders by `updated_at`
- `limit` The limit of how many pools should be retrieved.

This returns a JSON array, for each pool it returns:

- `id` The ID of the pool.
- `name` The name of the pool.
- `created_at` The time the pool was created in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.
- `updated_at` The time the pool was updated in the format of `YYYY-MM-DDTHH:MM:SS.MS+00:00`.
- `creator_id` the ID of the user that created the pool.
- `description` The description of the pool.
- `is_active` If the pool is active and stillg etting posts added. (True/False)
- `category` Can be "series" or "collection".
- `post_ids` An array group of posts in the pool.
- `creator_name` The name of the user that created the pool.
- `post_count` the amount of posts in the pool.

### Create

The base URL is `/pools.json` called with POST.

The pool's name and description are required, though the description can be empty.

- `pool[name]` The name of the pool.
- `pool[description]` The description of the pool.
- `pool[category]` Can be either "series" or "collection".
- `pool[is_locked]` 1 or 0, whether or not the pool is locked. Admin only function.

Success will return the pool in the format shown in the Listing section above.

### Update

The base URL is `/pools/<Pool_ID>.json` called with PUT.

Only post parameters you want to update.

- `pool[name]` The name of the pool.
- `pool[description]` The description of the pool.
- `pool[post_ids]` List of space delimited post ids in order of where they should be in the pool.
- `pool[is_active]` Can be either 1 or 0
- `pool[category]` Can be either "series" or "collection".

Success will return the pool in the format shown in the Listing section above.

### <span id="pools_revert"></span>Revert

The base URL is `/pools/<Pool_ID>/revert.json` called with PUT.

- `version_id` The version ID to revert to.

## API Endpoint Summary <span id="endpoints"></span>

| Function                             | Endpoint                       | Method |
|--------------------------------------|--------------------------------|--------|
| Search posts                         | /posts.json                    | GET    |
| Upload a new post                    | /uploads.json                  | POST   |
| Update a post                        | /posts/\<Post_ID\>.json        | PATCH  |
| Search flags                         | /post_flags.json               | GET    |
| Create a new flag                    | /post_flags.json               | POST   |
| Vote on a post                       | /posts/\<Post_ID\>/votes.json  | POST   |
| Favorite a post                      | /favorites.json                | POST   |
| Delete a favorite                    | /favorites/\<Post_ID\>.json    | DELETE |
| Search notes                         | /notes.json                    | GET    |
| Create a new note                    | /notes.json                    | POST   |
| Update an existing note              | /notes/\<Note_ID\>.json        | PUT    |
| Delete a note                        | /notes/\<Note_ID\>.json        | DELETE |
| Revert a note to a previous version  | /notes/\<Note_ID\>/revert.json | PUT    |
| Search pools                         | /pools.json                    | GET    |
| Create a new pool                    | /pools.json                    | POST   |
| Update pool                          | /pools/\<Pool_ID\>.json        | PUT    |
| Revert pool to some previous version | /pools/\<Pool_ID\>/revert.json | PUT    |

