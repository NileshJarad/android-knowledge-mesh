# GIT

### Clone

``git clone <repo-url>`` - Clone repo with entire history

``git clone --depth=1 <repo-url>`` - clone repo with last one commit history

``git checkout -b <branch-name> --single-branch <repo-url>`` - clone repo with specified branch only

## Status

``git status`` - prints current status of repo/ branch

## Checkout

``git checkout -b <branch-name>`` - checkout branch

``git switch <branch-name>`` - checkout branch

### Changes and Commit

``git add <file-name>`` - add files to staged area

``git commit -m "Message goes here"`` - add commit with message specified

``git commit --amend -m "Amend message"`` - Combines current and previous commit (it changes hashcode)

### Push

`` git push --set-upstream <remote> <branch name>`` - push and add tracking of local branch to remote

``git push origin`` -- push the changes to remote named origin you can specify branch name to push

``git push -f`` - force push the branch

### Log

``git log --oneline`` - Show one-line git logs

``git log --oneline --after = "last week"`` - shows logs that are committed after last week

``git log --oneline --grep="animation""`` - search for commit message that contains **animation**

``git log -n 2`` - shows last 2 commits only

### History re-write

``git reset --hard origin/main`` - uncommit + unstage + delete changes, nothing left.

``git reset --soft origin/main`` - uncommit changes, changes are left staged

``git rebase --interactice HEAD~N`` - squash last N commits

### Clean up files

`` git clean -nd`` - shows what files/directory going to be deleted in clean command

- -n list only files
- -nd list the directory

``git clean -xdf``

- The -x flag removes ignored files.
- The -d flag removes untracked folders.
- The -f flag removes untracked files.

### Stash

``git stash`` - Stash current changes (tracked files)
``git stash -u`` - Stash everything including untracked files
``git stash list`` - View all stash list
``git stash show`` - Show the changes in last stash
``git stash show -p`` - See full diff of latest stash
``git stash apply`` - Apply the latest stash, but keep it saved
``git stash apply stash@{1}`` - Apply a specific stash entry
``git stash pop`` - Apply and delete latest stash
``git stash drop stash@{0}`` - Delete a specific stash
``git stash clear`` - Delete all stashes




