from enum import Enum

class SpecType(Enum):
    """Enum for spec type."""
    CYCLONEDX = 'cyclonedx'
    SPDX = 'spdx'
    JBOM = 'jbom'
    SWID = 'swid'
    SYFT = 'syft'
    SCANCODE = 'scancode'

    
    def __str__(self):
        return self.value