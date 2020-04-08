package cromwell.pipeline.service

import cromwell.languages.util.ImportResolver.{ DirectoryResolver, HttpResolver }
import cromwell.pipeline.womtool.WomTool

class WomToolModule {
  private lazy val importResolvers = DirectoryResolver.localFilesystemResolvers(None) :+ HttpResolver(relativeTo = None)
  lazy val womTool: WomTool = new WomTool(importResolvers)
}
