import re
import os

from abstract_transformer import AbstractTransformer

class SPDXRDFTransformer(AbstractTransformer):
    def __init__(self, input_file, output_file=None):
        self.input_file = input_file
        self.output_file = output_file
        self.dependency_graph = []
        self.dependency_root = None
    
    def transform(self):
        rdf_output = self.__get_parsed_rdf_output(self.input_file)
        self.__construct_dependency_graph(rdf_output)

        flattened_dependencies = self.__flatten_dependencies(rdf_output[1:])

        self.write(flattened_dependencies, self.output_file)
    

    def __flatten_dependencies(self, rdf_output):
        flattened_dependencies = []
        for package in rdf_output:
            dependency_attributes = self.__get_dependency_attribute(package)
            depth = self.__compute_depth(self.__get_spdx_id(package))
            flattened_dependencies.append({**dependency_attributes, 'depth': depth})

        return flattened_dependencies
    
    def __get_spdx_id(self, package):
        for attribute in package.split(os.linesep):
            if attribute.startswith('SPDXID'):
                return attribute.removeprefix('SPDXID: ').strip()


    def __get_dependency_attribute(self, package):
        dependency_attributes = {}
        for attribute in package.split(os.linesep):
            if attribute.startswith('PackageName'):
                dependency_attributes['artifactId'] = attribute.removeprefix('PackageName: ').strip()
            elif attribute.startswith('PackageVersion'):
                dependency_attributes['version'] = attribute.removeprefix('PackageVersion: ').strip()
            elif attribute.startswith('PackageSupplier: Organization:'):
                dependency_attributes['groupId'] = attribute.removeprefix('PackageSupplier: Organization: ').strip()
        
        return dependency_attributes
    

    def __compute_depth(self, package_spdx_ref, depth=0):
        for relationship in self.dependency_graph:
            dependents = relationship.get('dependsOn', [])
            if package_spdx_ref in dependents:
                return self.__compute_depth(relationship['ref'], depth + 1)

        return depth   

    def __construct_dependency_graph(self, rdf_output):
        self.__get_root_package(rdf_output[0])
        for package in rdf_output:
            self.__parse_rdf_string(package)



    def __get_root_package(self, rdf_string):
        for attribute in rdf_string.split(os.linesep):
            if attribute.startswith('SPDXID'):
                self.dependency_root = attribute.removeprefix('SPDXID: ').strip()
        

    def __parse_rdf_string(self, rdf_string):
        for attribute in rdf_string.split(os.linesep):
            if attribute.startswith('Relationship') and 'DEPENDS_ON' in attribute:
                parent, child = attribute.removeprefix('Relationship: ').split('DEPENDS_ON')
                parent = parent.strip()
                child = child.strip()


                for dependency in self.dependency_graph:
                    if dependency['ref'] == parent:
                        dependency['dependsOn'].append(child)
                        break
                else:
                    self.dependency_graph.append({
                        'ref': parent,
                        'dependsOn': [child]
                    })


    @staticmethod
    def __get_parsed_rdf_output(input_file):
        regex = (r"PackageName:.*\n"
            r"SPDXID: .*\n"
            r"PackageVersion: .*\n"
            r"PackageSupplier: Organization: .*\n"
            r"PackageDownloadLocation: .*\n"
            r"FilesAnalyzed: .*\n"
            r"PackageChecksum: SHA1: .*\n"
            r"PackageHomePage: .*\n"
            r"PackageLicenseConcluded: .*\n"
            r"PackageLicenseDeclared: .*\n"
            r"PackageCopyrightText: .*\n"
            r"PackageLicenseComments: .*\n"
            r"PackageComment: .*\n"
            r"^[^#]*"
        )

        with open(input_file, 'r') as f:
            rdf_output = f.read()
            matches = re.findall(regex, rdf_output, re.MULTILINE)
            return matches