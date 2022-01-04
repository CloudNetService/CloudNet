# Contributing

We would like to make participation in this project as simple and transparent as possible.

## Our Development Process

We use GitHub as our main place where we keep our code. This is where we will track issues and feature requests and
accept pull requests. A new version will be available when all issues targeting the development version have been worked
on and closed.

## Pull Requests

We actively welcome your pull requests.

1. Fork the repo.
2. Be sure to target the development branch.
3. If you have added code that should be tested, add tests.
4. Ensure that all new and existing tests are passing.
5. If you changed something that should be documented, update the documentation.

## Coding Style

* See [the Google java style guide](https://google.github.io/styleguide/javaguide.html) for detailed info
* Run `gradle checkstyleMain checkstyleTest` to check if you are following all rules.

## Issues

We use GitHub issues to track public bugs. Please ensure your description is clear and has sufficient instructions to be
able to reproduce the issue. Be sure to fill all requested information from the issue template.

## Labels

Here are some labels that we are using to organize issues:

* `status: confirmed` - We can confirm that the issue actually exists and is reproducible.
* `status: inactive` - The issue had no recent activity. Not a good candidate to bump.
* `status: needs triage` - The issue is waiting for a maintainer to take a closer look at.
* `status: waiting for feedback` - Additional information was requested, and we are waiting for feedback.
* `wontfix` - The issue will not get fixed. See comments for additional details.
* `feature_request` - The issue is a new feature someone wants to see in a future release.
* `bug` - A report about a bug someone caught while using the system.
* `in: *` - Describes the issue location, for example: `in: node`.
* `target: *` - Describes the target release version for the issue to get fixed/implemented.

## API stability

* We will avoid changing APIs and behaviour when possible.
* If api breakage is required all methods which were replaced are annotated as:
  * `@Deperecated`
  * `@ScheduledForRemoval` - the removal version will be documented here. (Normally the next but one minor version).
* The documentation of changed methods will contain the new method you should use instead.

## Documentation

* Always include documentation on newly added methods. All parameters and exceptions must get documented. For new
  methods include a `@since` tag with the next release version.
* Feel free to include yourself as an author of the class.

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 license.
