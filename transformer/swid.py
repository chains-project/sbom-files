import xml.etree.ElementTree as ET

from abstract_transformer import AbstractTransformer

class SWIDTransformer(AbstractTransformer):
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
    
    def transform(self):
        xml_tree = self.__get_xml_tree(self.input_file)
        flattened_dependencies = self.__flatten_dependencies(xml_tree)

        self.write(flattened_dependencies, self.output_file)
    
    def __flatten_dependencies(self, xml_tree) -> list:
        flattened_dependencies = []

        # XPath: /SoftwareIdentity/Payload/Directory/Directory/File
        dependency_list = [dependency for dependency in xml_tree.getroot()[1][0][1]]

        for dependency in dependency_list:
            flattened_dependencies.append(self.__get_dependency_attribute(dependency))
        
        return flattened_dependencies
    
    
    def __get_dependency_attribute(self, file_element):
        version = file_element.attrib['version']
        artifact_id = file_element.attrib['name'].split(version)[0][:-1]
        return {
            'artifactId': artifact_id,
            'version': version,
            'groupId': None,
        }


    def __get_xml_tree(self, xml_file):
        return ET.parse(xml_file)