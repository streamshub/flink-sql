# Releasing

Currently the only external artefact for a release is the SQL runner image in quay.io.

The automation builds and pushes images to quay on push to:
1. main (image will be tagged as main eg quay.io/streamshub/flink-sql-runner:main )
2. branches named release-** (image will be tagged as branch name eg release-0.0 will be tagged at quay.io/streamshub/flink-sql-runner:release-0.0)
3. semver-like tag push (image will be tagged with the git tag, for example git tag 0.0.1 -> quay.io/streamshub/flink-sql-runner:0.0.1)

For the branches targeted above we can use github Actions to transition them between two states:

1. Snapshot, where the maven versions are SNAPSHOT and the image references in the deployments use the branch name as image tag.
2. Release, where the maven version are non-SNAPSHOT and the image references in the deployments point at a tagged image.

## Prerequisites
You must have permissions to execute manual GitHub Actions workflows and the ability to push tags to this repository

The repository must have a `RELEASE_PAT` secret containing a non-expired GitHub Personal Access Token with write permissions for this repositories contents and PRs.

## To Release a Branch that is in Development

1. Run the [Create Release Transition PR workflow](https://github.com/streamshub/flink-sql/actions/workflows/release-transition.yaml) setting:
  - Branch to transition: the branch you want to release (typically main)
  - New Version: if your development branch is on 0.0.1-SNAPSHOT in the maven projects, you would set this to 0.0.1
  - Choose: `DEVELOPMENT_TO_RELEASE`
2. This will create a PR. After CI has run against this PR, review, approve and merge it.
3. Fetch the branch changes locally and tag the merge commit as `${version}`. So if you are releasing `0.0.1`, run `git tag -a 0.0.1 -m 0.0.1` and push the tag up.
4. This tag push will trigger [integration.yaml](https://github.com/streamshub/flink-sql/actions/workflows/integration.yaml) to push a `0.0.1` tagged image to quay.io,
  matching the references in the deployment YAML that were set in the transition PR.

## To Transition a Branch back to Development after Release

1. Run the [Create Release Transition PR workflow](https://github.com/streamshub/flink-sql/actions/workflows/release-transition.yaml) setting:
  - Branch to transition: the branch you want to release (typically main)
  - New Version: the next development version, so if you released 0.0.1 this might now be 0.0.2-SNAPSHOT
  - Choose: `RELEASE_TO_DEVELOPMENT`
2. This will create a PR. After CI has run against this PR, review, approve and merge it.
3. The branch will now have the pom versions set to your new version (like 0.0.2-SNAPSHOT)

## Working with Backports/Bugfixes

If we ever needed to release an older version for some reason (like we wanted to put out a bugfixed 0.0.2 but main has moved far ahead)

1. Branch off the tag you want to work from, the release branch name must start with `release-`. So if we discovered a bug in 0.0.1 and wanted
to release a new 0.0.2 containing the bugfix, we could run  `git branch -b release-0.0.2 0.0.1` and push `release-0.0.2` to github.
2. Transition the new `release-0.0.2` branch to Development following the process above,  setting the branch to `release-0.0.2` in the action
and the new version to 1.0.2-SNAPSHOT
3. Make code changes
4. Release the branch following the process above, setting the branch to `release-0.0.2` in the action and new version `0.0.2`
5. As above, tag the latest commit in `release-0.0.2` as `0.0.2` and push it up. The automation will produce an image tagged 0.0.2 in quay.io
