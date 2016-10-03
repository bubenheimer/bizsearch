BizSearch
========

Find businesses around you.

I was asked to provide this as a sample app to demonstrate some concepts. The requirements I was
asked to use are not valid for a production app as the APK will necessarily
expose unrestricted API keys for REST calls. A better approach would rely on a server as a proxy
for web calls to encapsulate the keys.

### Google API Key is required

In order for this app to build, a Google API key must be set as the value of the Gradle property
BIZSEARCH_GOOGLE_API_KEY. The API key must include access to Maps and Places APIs and cannot be
restricted to Android build keys, as it is used for plain REST calls.
