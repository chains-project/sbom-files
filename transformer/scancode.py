from typing import Dict

from packageurl import PackageURL

from abstract_transformer import AbstractTransformer

class ScancodeTransformer(AbstractTransformer):
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
        self.root_id = None
    
    def transform(self):
        json_dict_input = self.get_json_as_dict(self.input_file)
        flattened_dependencies = self.__flatten_dependencies(json_dict_input)
        
        self.write(flattened_dependencies, self.output_file)
    
    def __flatten_dependencies(self, json_dict) -> list:
        flattened_dependencies = []
        
        dependencies = json_dict['dependencies']

        for dependency in dependencies:
            dependency_attributes = self.__get_dependency_attributes(dependency)
            # ToDo - compute depth after looking at SBOOMs of multi module projects
            flattened_dependencies.append(dependency_attributes)
        
        return flattened_dependencies
    

    def __get_dependency_attributes(self, dependency) -> Dict:
        dependency_purl = PackageURL.from_string(dependency['purl']).to_dict()

        return {
            'groupId': dependency_purl['namespace'],
            'artifactId': dependency_purl['name'],
            'version': dependency_purl['version'],
            'scope': dependency['scope'] if 'scope' in dependency else None,
        }