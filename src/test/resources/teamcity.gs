object Id: string

object Name: string

filter Archived: bool

object Type: string

object Project {
	id: Id [source]
	name: Name [source]
	feature: Feature [many, rev]
	vcs_root: VcsRoot [many, rev]
	archived: Archived
    project: Project [many, rev]
    build_conf: BuildConf [many, rev]
    template: Template [many, rev]
}

interface CommonBuildConf {
	id: Id [source]
	name: Name [source]
	trigger: Trigger [many] (withInherited)
	step: Step [many] (withInherited)
	param: Param [many] (withInherited, resolved)
	dep: Dependency [many]
	vcs_entry: VcsRootEntry [many]
	feature: Feature [many]
}

object BuildConf: CommonBuildConf

object Dependency: CommonBuildConf {
	artifact: ArtifactDependency [many]
	snapshot: SnapshotDependency
}

filter ArtifactDependency {
	rules: Rules [many] (resolved)
}

filter SnapshotDependency {
    option: Option [many]
}

source object Template {
	inheritedBy: Conf [rev, many]
	id: Id [source]
	name: Name [source]
	trigger: Trigger [many]
	step: Step [many]
	param: param [many] (resolved)
	dep: Dependency [many]
	vcs_entry: VcsRootEntry [many]
	feature: Feature [many]
}

object Trigger {
	type: Type
}

interface CommonVcsRoot {
	id: Id
	name: Name
	param: Param [many] (resolved)
	type: Type
}

source object VcsRoot: CommonVcsRoot

object VcsRootEntry: CommonVcsRoot  {
	rules: Rule [many]
}

object Step {
	type: Type
}

object Feature {
	type: Type
}

filter Enabled: bool

object Param {
	name: string
	value: string
} `{name} = {value}`

object Rule: string

filter Clean: bool

modifier resolved

modifier withInherited
