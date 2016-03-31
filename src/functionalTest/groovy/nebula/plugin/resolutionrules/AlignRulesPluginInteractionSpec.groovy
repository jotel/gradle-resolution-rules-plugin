/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nebula.plugin.resolutionrules

import nebula.test.IntegrationSpec
import nebula.test.dependencies.DependencyGraphBuilder
import nebula.test.dependencies.GradleDependencyGenerator

class AlignRulesPluginInteractionSpec extends IntegrationSpec {
    def 'alignment interaction with dependency-recommender'() {
        def graph = new DependencyGraphBuilder()
                .addModule('test.a:a:1.42.2')
                .build()
        def mavenrepo = new GradleDependencyGenerator(graph, "${projectDir}/testrepogen")
        mavenrepo.generateTestMavenRepo()

        def rulesJsonFile = new File(projectDir, 'rules.json')

        rulesJsonFile << '''\
            {
                "deny": [], "reject": [], "substitute": [], "replace": [],
                "align": [
                    {
                        "group": "test.nebula",
                        "reason": "Align test.nebula dependencies",
                        "author": "Example Person <person@example.org>",
                        "date": "2016-03-17T20:21:20.368Z"
                    }
                ]
            }
        '''.stripIndent()

        buildFile << """\
            buildscript {
                repositories { jcenter() }

                dependencies {
                    classpath 'com.netflix.nebula:nebula-dependency-recommender:3.1.0'
                }
            }

            ${applyPlugin(ResolutionRulesPlugin)}
            apply plugin: 'java'
            apply plugin: 'nebula.dependency-recommender'

            repositories {
                ${mavenrepo.mavenRepositoryBlock}
            }

            dependencyRecommendations {
               map recommendations: ['test.a:a': '1.42.2']
            }

            dependencies {
                resolutionRules files('$rulesJsonFile')
                compile 'test.a:a'
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compile')

        then:
        result.standardOutput.contains '\\--- test.a:a: -> 1.42.2\n'
    }

    def 'alignment interaction with dependency-recommender reverse order of application'() {
        def graph = new DependencyGraphBuilder()
                .addModule('test.a:a:1.42.2')
                .build()
        def mavenrepo = new GradleDependencyGenerator(graph, "${projectDir}/testrepogen")
        mavenrepo.generateTestMavenRepo()

        def rulesJsonFile = new File(projectDir, 'rules.json')

        rulesJsonFile << '''\
            {
                "deny": [], "reject": [], "substitute": [], "replace": [],
                "align": [
                    {
                        "group": "test.nebula",
                        "reason": "Align test.nebula dependencies",
                        "author": "Example Person <person@example.org>",
                        "date": "2016-03-17T20:21:20.368Z"
                    }
                ]
            }
        '''.stripIndent()

        buildFile << """\
            buildscript {
                repositories { jcenter() }

                dependencies {
                    classpath 'com.netflix.nebula:nebula-dependency-recommender:3.1.0'
                }
            }

            apply plugin: 'nebula.dependency-recommender'
            ${applyPlugin(ResolutionRulesPlugin)}
            apply plugin: 'java'


            repositories {
                ${mavenrepo.mavenRepositoryBlock}
            }

            dependencyRecommendations {
               map recommendations: ['test.a:a': '1.42.2']
            }

            dependencies {
                resolutionRules files('$rulesJsonFile')
                compile 'test.a:a'
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compile')

        then:
        result.standardOutput.contains '\\--- test.a:a: -> 1.42.2\n'
    }

    def 'align rules work with spring-boot'() {
        def rulesJsonFile = new File(projectDir, 'rules.json')
        rulesJsonFile << '''\
            {
                "deny": [], "reject": [], "substitute": [], "replace": [],
                "align": [
                    {
                        "group": "test.nebula",
                        "reason": "Align test.nebula dependencies",
                        "author": "Example Person <person@example.org>",
                        "date": "2016-03-17T20:21:20.368Z"
                    }
                ]
            }
        '''.stripIndent()

        buildFile << """\
            buildscript {
                repositories { jcenter() }
                dependencies {
                    classpath('org.springframework.boot:spring-boot-gradle-plugin:1.3.3.RELEASE')
                }
            }
            apply plugin: 'spring-boot'
            ${applyPlugin(ResolutionRulesPlugin)}

            repositories { jcenter() }

            dependencies {
                resolutionRules files('$rulesJsonFile')
                compile('org.springframework.boot:spring-boot-starter-web')
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully('dependencies', '--configuration', 'compile')

        then:
        noExceptionThrown()
    }
}
