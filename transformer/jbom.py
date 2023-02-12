from typing import Dict

from abstract_transformer import AbstractTransformer

class JBOMTransformer(AbstractTransformer):
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

        for component in components:
            dependency_attributes = self.__get_dependency_attribute(component)
            flattened_dependencies.append(dependency_attributes)

        return flattened_dependencies


    def __get_dependency_attribute(self, component_dict) -> Dict:
        return {
            'groupId': component_dict['group'] if 'group' in component_dict else None,
            'artifactId': component_dict['name'],
            'version': component_dict['version'],
            'scope': component_dict['scope'] if 'scope' in component_dict else None,
        }
