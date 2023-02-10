import json
from typing import Dict

from packageurl import PackageURL

class CycloneDXTransformer:
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
    

    def transform(self):
        json_dict_input = self.__get_json_as_dict(self.input_file)
        flattened_dependencies = self.__flatten_dependencies(json_dict_input)

        self.__write(flattened_dependencies, self.output_file)
    

    def __flatten_dependencies(self, json_dict) -> list:
        flattened_dependencies = []

        components = json_dict['components']

        dependency_relationships = json_dict['dependencies']
        for component in components:
            dependency_attributes = self.__get_dependency_attribute(component)
            depth = self.__compute_depth(dependency_relationships, component['bom-ref'])
            flattened_dependencies.append({**dependency_attributes, 'depth': depth})

        return flattened_dependencies
    
    def __compute_depth(self, dependency_relationships, component_purl, depth=0) -> int:
        for relationship in dependency_relationships:
            dependents = relationship['dependsOn']
            if component_purl in dependents:
                return self.__compute_depth(dependency_relationships, relationship['ref'], depth + 1)

        return depth                

    def __get_dependency_attribute(self, component_dict) -> Dict:
        if 'purl' in component_dict:
            component_purl = PackageURL.from_string(component_dict['purl']).to_dict()
            return {
                'groupId': component_dict['group'],
                'artifactId': component_dict['name'],
                'classifier': component_purl['qualifiers']['type'],
                'version': component_dict['version'],
                'scope': component_dict['scope'] if 'scope' in component_dict else None,
            }
        else: # jFrog specific
            # Also includes the project itself in components
            return {
                'groupId': component_dict['group'],
                'artifactId': component_dict['name'],
                'version': component_dict['version'],
            }


    
    def __get_json_as_dict(self, input_file) -> dict:
        with open(input_file) as f:
            return json.loads(f.read())
    
    def __write(self, json_dict, output_file) -> None:
        if output_file is None:
            print(json.dumps(json_dict, indent=2))
        else:
            with open(output_file, 'w') as f:
                f.write(json.dumps(json_dict, indent=2))
