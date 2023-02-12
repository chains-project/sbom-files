from typing import Dict

from abstract_transformer import AbstractTransformer

class SyftTransformer(AbstractTransformer):
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

        artifacts = json_dict['artifacts']

        self.root_id = json_dict['source']['id']

        artifact_relationships = json_dict['artifactRelationships']

        for artifact in artifacts:
            dependency_attributes = self.__get_dependency_attribute(artifact)
            depth = self.__compute_depth(artifact, artifact_relationships)
            flattened_dependencies.append({**dependency_attributes, 'depth': depth})

        return flattened_dependencies


    def __compute_depth(self, artifact, artifact_relationships, depth=0) -> int:
        for relationship in artifact_relationships:
            if relationship['child'] == artifact['id']:
                depth += 1

                if relationship['parent'] == self.root_id:
                    return depth
        
                return self.__compute_depth(relationship['parent'], artifact_relationships, depth + 1)
        
        return -1
        


    def __get_dependency_attribute(self, artifact_dict) -> Dict:
        return {
            'groupId': artifact_dict['metadata']['pomProperties']['groupId'],
            'artifactId': artifact_dict['name'],
            'version': artifact_dict['version'],
        }