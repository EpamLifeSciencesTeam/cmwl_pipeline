defaultLinuxInstallLocation in Docker := sys.env.getOrElse("CMWL_PIPELINE_CONTAINER_PATH", "/cmwl_pipeline/app")
packageName in Docker := sys.env.getOrElse("CMWL_PIPELINE_CONTAINER_NAME", "cmwl_pipeline")
dockerExposedVolumes := Seq(sys.env.getOrElse("CMWL_PIPELINE_CONTAINER_LOGS", "/cmwl_pipeline/app/logs"))
dockerExposedPorts := Seq(sys.env.getOrElse("CMWL_PIPELINE_CONTAINER_PORT", "8080").toInt)
