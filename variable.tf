locals {
    env = {
        DEV ="dev"
        PREPROD = "preprod"
        PROD = "prod"
    }
    frontend_instance_count = {
        DEV = "1"
        PREPROD = "2"
        PROD = "2"
    }
    backend_instance_count = {
        DEV = "1"
        PREPROD = "2"
        PROD = "2"
    }

    cidr_backend = {
        DEV = "10.10.10.0/24"
        PREPROD = "10.10.20.0/24"
        PROD = "10.10.30.0/24"
    }
    cidr_frontend = {
        DEV = "10.10.40.0/24"
        PREPROD = "10.10.50.0/24"
        PROD = "10.10.60.0/24"
    }
}

variable region {
    default = "europe-west1"
}
variable zone {
    default = ["europe-west1-b","europe-west1-c","europe-west1-d"]
}

variable google_prj {
    default = "spry-analyzer-368916"
}