from typing import List

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

        dependency_relationships = json_dict['dependencies']


        for component in dependency_relationships:
            dependency_attributes = self.__get_dependency_attribute(component, json_dict['components'], dependency_relationships)
            flattened_dependencies.extend(dependency_attributes)

        return flattened_dependencies


    def __get_dependency_attribute(self, component_dict, components, dependency_relationships) -> List:
        dependencies = []
        ref_attributes = self.__get_gav(component_dict['ref'])
        dependsOn = component_dict['dependsOn']

        ref_dependency = {
            'groupId': ref_attributes[0],
            'artifactId': ref_attributes[1],
            'version': ref_attributes[2],
            'depth': self.__compute_depth(dependency_relationships, component_dict['ref']),
            'scope': self.__get_scope(ref_attributes, components)
        }

        dependencies.append(ref_dependency)

        for dependent in dependsOn:
            dependent_attributes = self.__get_gav(dependent)
            dependencies.append({
                'groupId': dependent_attributes[0],
                'artifactId': dependent_attributes[1],
                'version': dependent_attributes[2],
                'depth': self.__compute_depth(dependency_relationships, dependent),
                'scope': self.__get_scope(dependent_attributes, components)
            })
        
        return dependencies
    

    def __compute_depth(self, dependency_relationships, id, depth=0) -> int:
        for relationship in dependency_relationships:
            dependents = relationship.get('dependsOn', [])
            if id in dependents:
                return self.__compute_depth(dependency_relationships, relationship['ref'], depth + 1)

        return depth

    def __get_gav(self, dependency) -> str:
        return dependency.split(':')
    

    def __get_scope(self, attributes, components):
        artifactId = attributes[1]
        version = attributes[2]


        for component in components:
            if component['name'] == artifactId and component['version'] == version:
                return component['scope'] if 'scope' in component else None

