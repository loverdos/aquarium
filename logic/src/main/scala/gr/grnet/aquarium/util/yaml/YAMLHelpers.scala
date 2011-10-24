package gr.grnet.aquarium.util.yaml

import org.yaml.snakeyaml.Yaml
import java.io.{StringReader, InputStreamReader, Reader, InputStream}

/**
 * Utility methods for parsing YAML and conveniently returning `YAMLNode`s.
 *
 * @author Christos KK Loverdos <loverdos@gmail.com>.
 */
object YAMLHelpers {
  def loadYAML(r: Reader, closeReader: Boolean = true): YAMLNode = {
    val yaml = new Yaml()
    val loaded = yaml.load(r)
    if(closeReader) r.close()

    YAMLNode(loaded)
  }
}