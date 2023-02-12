from typing import Dict

from packageurl import PackageURL

from abstract_transformer import AbstractTransformer

# Could not understand how to interpret dependency graph in ORT output
class ORTTransformer(AbstractTransformer):
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
    
    def transform(self):
        json_dict_input = self.get_json_as_dict(self.input_file)
        flattened_dependencies = self.__flatten_dependencies(json_dict_input)
        
        self.write(flattened_dependencies, self.output_file)
    
    def __flatten_dependencies(self, json_dict) -> list:
        flattened_dependencies = []
        
        dependencies = json_dict['analyzer']['result']['packages']
        dependency_relationships = json_dict['analyzer']['result']['dependency_graphs']
        for dependency in dependencies:
            dependency_attributes = self.__get_dependency_attributes(dependency, dependency_relationships['Maven'])
            flattened_dependencies.append(dependency_attributes)
        
        return flattened_dependencies
    
    def __get_dependency_attributes(self, dependency, maven_dependencies) -> Dict:
        dependency_purl = PackageURL.from_string(dependency['purl']).to_dict()
        scope = self.__get_scope(dependency['id'], maven_dependencies)
        return {
            'groupId': dependency_purl['namespace'],
            'artifactId': dependency_purl['name'],
            'version': dependency_purl['version'],
            'scope': scope,
        }
    

    def __get_scope(self, dependency_id, maven_dependencies) -> str:
        index_of_dependency = maven_dependencies['packages'].index(dependency_id)

        for scope in maven_dependencies['scopes']:
            for dependency_map_id in maven_dependencies['scopes'][scope]:
                if dependency_map_id['root'] == index_of_dependency:
                    return scope.split(':')[3]

        return None
