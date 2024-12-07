tasks.withType<Jar> {
  archiveFileName.set(Files.cloudflare)
}

dependencies {
  "compileOnly"(libs.bundles.unirest)
  "compileOnly"(projects.node.nodeImpl)
  "compileOnly"(projects.utils.utilsBase)

  "implementation"(projects.modules.cloudflare.cloudflareApi)

}

moduleJson {
  author = "CloudNetService"
  name = "CloudNet-CloudFlare"
  main = "eu.cloudnetservice.modules.cloudflare.impl.CloudNetCloudflareModule"
  description = "Node extension for automatic creation of SRV entries for proxy services"
  storesSensitiveData = true
}
