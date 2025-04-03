# Main

## Design Philosophy

This class follows a Unix-style command dispatching pattern. The main method receives command-line arguments, determines the command type based on the first argument, and checks the argument length. All Gitlet commands (e.g., init, add, commit, etc.) are dispatched explicitly to the Repository class via switch-case, decoupling the entry point from specific command logic and maintaining a clear structure.

To improve robustness, Main validates the command format via validateArgs() and catches any GitletException thrown during execution to present user-friendly error messages.

## Implementation Details

- Command Parsing and Dispatch:
    - Checks if a command is provided (args.length == 0);
    - Uses Java 12 enhanced switch-case syntax to dispatch by the first argument;
    - All commands except init must run within a .gitlet repository directory, otherwise an exception is thrown.

- Argument Validation with validateArgs():
    - Verifies the number of arguments falls within the required range;
    - The checkout command requires a special format (with the "--" separator);
    - Calls validatePath() to ensure the current directory is a valid Gitlet repository.

- Supported Commands:
    - Local: init, add, commit, rm, log, global-log, find, status, checkout, branch, rm-branch, reset, merge
    - Remote: add-remote, rm-remote, push, fetch, pull
    - All command logic is implemented in the Repository class.

- Error Handling:
    - All exceptions are wrapped with GitletException and printed as friendly messages before the program exits.

## Key Methods

- main(String[] args): Main entry point and command dispatcher.
- validateArgs(String[] args, int min, int max): Validates argument count and format.
- validatePath(): Ensures current directory is an initialized .gitlet repository.


# Repository

## Design Philosophy

The Repository class is designed as a highly modular controller interface. Each command (e.g., add, commit, checkout, merge) is handled by a static method, internally delegating to state managers (CommitManager, FileManager, MergeManager). This design ensures:
- Clear and separate handling for each command;
- Persistent state (commits, files, staging area) is loaded/saved from disk using readObject and save;
- Exceptions are uniformly thrown as GitletException, simplifying error handling.

## Implementation Details

- Initialization (setup)
    - Creates .gitlet/ directory structure (commits, blobs, staging);
    - Initializes and serializes CommitManager and FileManager.

- Versioning Operations
    - addFile(): Adds file to staging area unless unchanged.
    - remove(): Marks a file for removal and deletes it from working directory.
    - commit(): Creates a new commit based on the staging area and updates the branch pointer.
    - log() / globalLog() / find(): Traverse history or find commits by message.

- Branch Management
    - branch(): Creates a new branch.
    - rmBranch(): Deletes a branch (not the current one).
    - checkout(): Supports three modes—restore file, revert commit, switch branch.
    - reset(): Forcefully move HEAD to a specified commit, updating working directory.

- Merge
    - merge(): Uses MergeManager for three-way merge:
        - Finds split point;
        - Checks fast-forward and ancestor relationships;
        - Checks for untracked file conflicts;
        - Applies file operations (checkout/remove/conflict) based on status codes;
        - Creates a new merge commit with two parents.

- Status Display
    - status(): Shows current branches, staging area, untracked files, etc.;
    - Retrieves all file status from FileManager and CommitManager.

- Remote Repositories
    - addRemote() / rmRemote(): Manage remote repository addresses.
    - push(): Push commits to remote branch if remote is an ancestor.
    - fetch(): Pull commits/blobs from remote, create remote/xxx branch.
    - pull(): A combination of fetch and merge.

## Module Collaboration Diagram

                    User Command  
                       ↓  
                    Repository  
       ┌──────────┬────────────┬────────────┐  
       ↓          ↓            ↓            ↓  
    FileManager CommitManager MergeManager  Utils  
    (Staging)   (Commit DAG)  (Merge Logic) (Utilities)  

## Key Fields (Directory Structure)

- CWD: Working directory path (project root)
- GITLET_DIR: .gitlet directory
- STAGING_BLOBS: Staging blob folder
- BLOBS: Permanent blob storage directory
- COMMITS: Commit object storage directory
- COMMIT_MANAGER / FILE_MANAGER: Serialized paths for state managers

## Highlights

- Clear encapsulation and unified interface: each command handled via static method.
- Ensures state consistency by reloading state objects per command.
- Decoupled from underlying modules which manage persistent state and logic.


# Commit

## Design Philosophy

This class is inspired by Git’s immutable commit object. It is designed to be immutable (fields are final) so that each commit saves an independent snapshot of the file state. A unique commit ID is generated from its content to ensure traceability. The parentCommits field supports both single-parent and merge commits (two parents), forming a Directed Acyclic Graph (DAG).

## Implementation Details

- Commit Object Construction: Takes message, timestamp, parent list, and tracked files.
- ID Generation: createId() serializes all fields and computes a SHA1 hash.
- File Tracking:
    - trackedFile is a TreeMap mapping filenames to blob IDs.
    - updateTrackingFiles() applies additions/removals from staging.
    - trackFile() saves files to the blob directory from staging.
- State Query Methods:
    - isTracking(): Checks if file is tracked.
    - isTrackingSameIn(): Checks if tracked file is the same as in working directory.
    - isTrackingDifferentIn(): Checks if file content has changed.
- Creating Child Commit: childCommit() clones tracking state and sets self as parent.
- Persistent Storage: save() serializes and saves commit object to disk.

## Key Fields

- message: Commit message
- time: Commit timestamp (Instant)
- parentCommits: List of parent commit IDs (can be 2 for merges)
- trackedFile: filename → blob ID
- commitId: SHA1 hash of serialized content


# CommitManager

## Design Philosophy

This class tracks all commit IDs, branch names and their pointers, and the HEAD branch. It encapsulates all logic for storing, switching branches, finding split points, etc. Its state is saved under .gitlet/commitManager for persistence.

It also supports managing remote repository references (for fetch/push).

## Implementation Details

- Initialization:
    - Creates the initial commit and sets master as HEAD.
    - Initializes commits (ID → message), branches (name → commit ID), and remoteRepos (name → path).

- Commit and Persistence:
    - addCommit() adds a commit and saves it, updating the current branch.

- Branch Management:
    - createNewBranch(), removeBranch(), changeHeadTo(), setHeadCommit() manage branch pointers.

- Commit Retrieval:
    - getCommit() retrieves commit by full or prefix ID.
    - getBranchCommit(): returns latest commit of a branch.
    - findByMessage(): searches commits by message.

- Split Point Detection:
    - findSplitPoint(): uses BFS to find common ancestor (supports cross-repo).
    - getAllAncestors(): returns all ancestor commit IDs.

- Remote Support:
    - addRemoteRepo() and rmRemoteRepo() manage remote references.

## Key Fields

- savePath: Path to save this object (usually .gitlet/commitManager)
- commitDir: Directory where commits are saved
- commits: commit ID → message map
- branches: branch name → commit ID map
- headBranchName: current branch name
- remoteRepos: remote name → path map


# FileManager

## Design Philosophy

FileManager coordinates the working directory and staging area, facilitating file state inspection, staging updates, blob tracking, and file restoration.

Gitlet splits files into three regions: the working directory (CWD), the staging area, and the commit snapshot. FileManager unifies access and management of these regions, offering high-level APIs for modification detection, tracking, and recovery.

The staging area is split into `addition` and `removal` maps. Blob snapshots are identified by their SHA1 hash. FileManager itself is serializable to persist staging state.

## Implementation Details

- Initialization:
    - Receives workingDir, blobDir, stagingDir, and commitManagerPath;
    - Calls updateFiles() to build file management scope.

- Staging Management:
    - Uses addition (Map) and removal (Set) for staged changes.
    - addToAddition() hashes and stores working file into staging.
    - removeFromAddition(), addToRemoval() update staging records.
    - clearStageArea() resets state after commit.

- File State Detection:
    - hasModified(), hasDeleted(), isNotTracking() detect file status.
    - getStagedFiles(), getRemovedFiles(), getModifiedFiles(), getUntrackedFiles() return file lists.

- File Restoration (Checkout):
    - checkout(commit): restores all tracked files from commit to working directory.
    - checkout(commit, filename): restores a single file.

- Remote Support:
    - fetchBlobFrom(): fetches blob from remote if missing locally.

## Key Fields

- savePath: path to save this FileManager object
- workingDir: project working directory
- stagingBlobsDir: where blobs are staged before commit
- blobsDir: permanent blob snapshot storage
- commitManagerPath: used to get current HEAD commit
- addition: filename → blob ID for staged additions
- removal: set of filenames to be deleted
- filesInManagement: files in CWD, staging, and current commit


# MergeManager

## Design Philosophy

MergeManager encapsulates Gitlet’s complex merge logic including split point resolution, three-way file comparison, conflict detection, and application of file changes.

This class is completely separated from Repository to isolate merge logic. It uses a 3-digit status code (split-current-given) per file to classify file handling actions: checkout, remove, or conflict.

## Implementation Details

- Constructor Initialization:
    - Accepts three commits (splitPoint, current, given), untracked file set, working directory, and blob path;
    - Initializes checkoutFiles, removeFiles, conflictFiles, and untrackedFiles.

- File Status Analysis:
    - getAllFiles(): returns union of filenames tracked in all 3 commits.
    - statusCode(filename): returns 3-digit status encoding file presence and differences.

- Merge Logic:
    - merge(): iterates over all files and dispatches handling via merge(filename);
    - merge(filename) applies status rules:
        - 112, 001 → checkout and stage
        - 110 → mark for removal
        - 123, 120, 102, 012 → conflict

- File Operations:
    - doCheckout(): restores file from given commit to working dir and stages it.
    - doRemove(): marks file for deletion.
    - handleConflict(): writes conflict-marked content to working file and stages it.

- Conflict Detection:
    - encounteredConflict(): returns whether any conflict files exist.

## Status Code Example (split-current-given)

- “112” means:
    - file tracked in split (1)
    - same in current (1)
    - different in given (2)
    - This encoding is used to quickly match merging scenarios based on a decision table (as documented in the class comment).

## Key Fields

- splitPoint: lowest common ancestor commit
- currentCommit: current branch head
- givenCommit: target branch head to merge
- untrackedFiles: files in working dir not tracked by HEAD
- checkoutFiles: files to checkout and stage
- removeFiles: files to remove from repo
- conflictFiles: files with merge conflicts
- workingDir: project directory
- blobDir: blob storage path


# Utils

## Design Philosophy

Utils provides core utility methods for file operations, hashing, serialization, path handling, and error reporting. It is a static class used throughout Gitlet.

Utility operations are grouped into hash computation, I/O, object persistence, file system manipulation, and messaging.

## Implementation Details

- SHA-1 Hashing
    - sha1(...): accepts strings/byte arrays, returns SHA1 hash of concatenated content.

- File I/O
    - readContents(file), readContentsAsString(file): read file as bytes or string.
    - writeContents(file, ...): writes strings/bytes into file, overwriting it.

- Object Serialization
    - serialize(obj): converts object to byte array.
    - writeObject(file, obj): serializes and saves object to file.
    - readObject(file, type): deserializes and casts object from file.

- Path Handling
    - join(...): joins path segments to File object.

- File System Operations
    - plainFilenamesIn(dir): returns sorted list of regular file names.
    - restrictedDelete(file): deletes file only if in Gitlet repo.
    - clean(dir): deletes all regular files in dir.

- Messaging
    - error(), message(): formatted error and info output.

- Misc
    - fileHashIn(dir, filename): computes hash of file name + content.


# Helper Classes & Debugging Tools

## GitletException

GitletException is the unified runtime error class used in Gitlet to handle user-facing issues with descriptive messages.

### Design

It extends Java’s RuntimeException and is used to differentiate logic errors from system errors.

### Usage

- Provides default and message constructors.
- Often used with Utils.error() for formatted error throwing.

## DumpObj

DumpObj is a debugging tool that loads and prints serialized Gitlet objects like CommitManager or FileManager.

### Design

Accepts file paths as input, loads objects, and prints structure using each object’s dump() method.

### Usage

- Calls Utils.readObject(..., Dumpable.class).
- Expects all objects to implement Dumpable.
- Dumps structure to console.

## Dumpable

Dumpable is an interface for any serializable Gitlet object that can be inspected using DumpObj.

### Usage

- Extends Serializable.
- Requires a dump() method to print internal state.