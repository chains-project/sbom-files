from typing import Dict

from packageurl import PackageURL

from abstract_transformer import AbstractTransformer

class OpenRewriteTransformer(AbstractTransformer):
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
    

    def transform(self):
        json_dict_input = self.get_json_as_dict(self.input_file)
        flattened_dependencies = self.__flatten_dependencies(json_dict_input)

        self.write(flattened_dependencies, self.output_file)
    

    def __flatten_dependencies(self, json_dict) -> list:
        flattened_dependencies = []

        components = json_dict['components']

        dependency_relationships = json_dict['dependencies']
        for component in components:
            dependency_attributes = self.__get_dependency_attribute(component)
            depth = self.__compute_depth(dependency_relationships, component['bom-ref'])
            flattened_dependencies.append({**dependency_attributes, 'depth': depth})

        return flattened_dependencies
    
    def __compute_depth(self, dependency_relationships, component_purl, depth=1) -> int:
        for relationship in dependency_relationships:
            dependents = relationship.get('dependsOn', [])
            if component_purl in dependents:
                return self.__compute_depth(dependency_relationships, relationship['ref'], depth + 1)

        return depth                

    def __get_dependency_attribute(self, component_dict) -> Dict:
        component_purl = PackageURL.from_string(component_dict['purl']).to_dict()
        return {
            'groupId': component_dict['group'],
            'artifactId': component_dict['name'],
            'classifier': component_purl['qualifiers']['type'],
            'version': component_dict['version'],
            'scope': component_dict['scope'] if 'scope' in component_dict else None,
        }
