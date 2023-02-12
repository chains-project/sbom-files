from packageurl import PackageURL

from abstract_transformer import AbstractTransformer


class SPDXTranformer(AbstractTransformer):
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
    
    def transform(self):
        json_dict_input = self.get_json_as_dict(self.input_file)
        flattened_dependencies = self.__flatten_dependencies(json_dict_input)

        self.write(flattened_dependencies, self.output_file)
    
    def __flatten_dependencies(self, json_dict) -> list:
        flattened_dependencies = []

        components = json_dict['packages']

        for component in components:
            if component['SPDXID'] == "SPDXRef-RootPackage": # Skip root package because it's not a dependency
                continue
            dependency_attributes = self.__get_dependency_attribute(component)
            flattened_dependencies.append(dependency_attributes)

        return flattened_dependencies
    
    
    def __get_dependency_attribute(self, component_dict):
        group_id, artifact_id = self.__get_group_id_and_artifact_id(component_dict)
        return {
            "groupId": group_id,
            "artifactId": artifact_id,
            "version": component_dict['versionInfo'] if 'versionInfo' in component_dict else None,
        }
    

    def __get_group_id_and_artifact_id(self, component_dict):
        if 'externalRefs' in component_dict:
            external_ref = component_dict['externalRefs'][0]
            if external_ref['referenceType'] == 'purl':
                purl = PackageURL.from_string(external_ref['referenceLocator']).to_dict()
                return purl['namespace'], purl['name']
        
        return component_dict['originator'] if 'originator' in component_dict else None, component_dict['name']
