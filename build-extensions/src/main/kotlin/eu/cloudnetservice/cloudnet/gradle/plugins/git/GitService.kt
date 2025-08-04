/*
 * Copyright 2019-2025 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cloudnetservice.cloudnet.gradle.plugins.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.*
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.lang.AutoCloseable
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.useLines

abstract class GitService : BuildService<GitService.Params>, AutoCloseable {
  private val git: GitInstance = GitInstance(parameters.projectDirectory.get().asFile.toPath())

  override fun close() {
  }

  interface Params : BuildServiceParameters {
    val projectDirectory: DirectoryProperty
  }

  val branchName: String?
    get() = branch?.run { Repository.shortenRefName(this.name) }

  val commit: ObjectId?
    get() = head?.run { this.objectId }

  val head: Ref?
    get() = git.git?.repository?.run { exactRef(Constants.HEAD) }

  val branch: Ref?
    get() = head?.run { if (!this.isSymbolic) null else this.target }

  /**
   * Currently has no support for eu.cloudnetservice.cloudnet.gradle.git submodules.
   * We don't use them so it should be ok
   */
  private class GitInstance(private val dir: Path) : Closeable {
    companion object {
      val logger: Logger = LoggerFactory.getLogger(GitInstance::class.java)
      const val GIT_DIR = ".git"
      const val GITDIR_PREFIX = "gitdir:"
    }

    private val lock = ReentrantLock()
    private var opened = false
    private var closed = false
    private var wrapper: Wrapper? = null

    val git: Git?
      get() = resolve()?.git

    private fun resolve(): Wrapper? {
      lock.withLock {
        if (closed) return null
        if (opened) return wrapper
        opened = true

        var targetDir: Path? = dir
        while (targetDir?.resolve(GIT_DIR)?.notExists() ?: false) {
          targetDir = targetDir.parent
        }

        val gitDir = targetDir?.let { resolveGitDirectory(it) }
        if (targetDir == null || gitDir == null) {
          logger.warn("[Git] Unable to find repository for $dir")
          return null
        } else {
          val repository =
            RepositoryBuilder().setWorkTree(dir.toFile()).setGitDir(gitDir.toFile()).setMustExist(true).build()
          val git = Git.wrap(repository)
          this.wrapper = Wrapper(git, repository)
          return this.wrapper
        }
      }
    }

    private data class Wrapper(val git: Git, val repository: Repository) : Closeable {
      override fun close() {
        git.close()
        repository.close()
      }
    }

    private fun resolveGitDirectory(projectDir: Path): Path? {
      // https://git-scm.com/docs/gitrepository-layout
      // .eu.cloudnetservice.cloudnet.gradle.git file is allowed with 'gitdir:' reference
      projectDir.run {
        if (fileName.toString() == GIT_DIR) projectDir else projectDir.resolve(GIT_DIR)
      }.run {
        if (isDirectory()) return this
        this.useLines { sequence ->
          sequence.forEach { line ->
            if (line.startsWith(GITDIR_PREFIX)) {
              return parent.resolve(line.substring(GITDIR_PREFIX.length))
            }
          }
        }
        logger.warn("[Git] Could not determine eu.cloudnetservice.cloudnet.gradle.git directory for $projectDir")
        return null
      }
    }

    override fun close() {
      lock.withLock {
        if (closed) return
        closed = true
        wrapper?.close()
        wrapper = null
      }
    }
  }
}
