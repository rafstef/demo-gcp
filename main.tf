
terraform {
  backend "gcs" {
    bucket  = "202211-demo-tfstate"
    prefix  = "terraform/state"
    credentials = "credentials/gcp-credentials.json"
  }
}

provider "google" {
  project     = "spry-analyzer-368916"
  region      = "europe-west1"
  credentials = "credentials/gcp-credentials.json"
}



module "network" {
  source  = "terraform-google-modules/network/google"
  version = "5.2.0"
  project_id = var.google_prj
  network_name = "demo-${lookup(local.env, terraform.workspace)}"
  subnets = [
        {
            subnet_name           = "demo-backend-${lookup(local.env, terraform.workspace)}"
            subnet_ip             = "${lookup(local.cidr_backend, terraform.workspace)}"
            subnet_region         = "${var.region}"
        },
        {
            subnet_name           = "demo-frontend-${lookup(local.env, terraform.workspace)}"
            subnet_ip             = "${lookup(local.cidr_frontend, terraform.workspace)}"
            subnet_region         = "${var.region}"
            subnet_private_access = "true"
        }
    ]
}


resource "google_compute_firewall" "default" {
  name    = "demo-firewall-${lookup(local.env, terraform.workspace)}"
  network = module.network.network_self_link

  allow {
    protocol = "icmp"
  }

  allow {
    protocol = "tcp"
    ports    = ["22", "873"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags = ["demo-${lookup(local.env, terraform.workspace)}"]
}


#FRONTEND
resource "google_compute_instance" "frontend" {
  count = "${lookup(local.frontend_instance_count, terraform.workspace)}"
  name         = "demo-frontend-${lookup(local.env, terraform.workspace)}-${count.index}"
  machine_type = "n1-standard-1"
  zone         = "${var.zone[count.index]}"
  tags         = ["demo-${lookup(local.env, terraform.workspace)}"]
  boot_disk {
    initialize_params {
      image = "demo-ubuntu"
    }
  }

network_interface {
    network = module.network.network_self_link
    subnetwork = module.network.subnets_ids[count.index]
    access_config {
      nat_ip = google_compute_address.static[count.index].address
    }
  }
}

#BACKEND
resource "google_compute_address" "static" {
  count = 3
  name = "ipv4-address-${count.index}"
}

resource "google_compute_instance" "backend" {
  count = "${lookup(local.backend_instance_count, terraform.workspace)}"
  name         = "demo-backend-${lookup(local.env, terraform.workspace)}-${count.index}"
  machine_type = "n1-standard-1"
  zone         = "${var.zone[count.index]}"
  tags         = ["demo-${lookup(local.env, terraform.workspace)}"]

  boot_disk {
    initialize_params {
      image = "demo-ubuntu"
    }
  }

network_interface {
  network = module.network.network_self_link
  subnetwork = module.network.subnets_ids[count.index]
  }
}
