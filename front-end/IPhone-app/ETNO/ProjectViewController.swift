// Authors     : Luis Fernando
//               Kevin Legarreta
//               David J. Ortiz Rivera
//               Enrique Rodriguez
//
// File        : ProjectViewController.swift
// Description : View controller for project view that mostly prepares segues
//               and checks if user is admin of said project and displays files.
// Copyright © 2018 Los Duendes Malvados. All rights reserved.

import UIKit

class ProjectViewController: UIViewController, UINavigationControllerDelegate, UITableViewDelegate, UITableViewDataSource{
  
    // MARK: - Variables
    // MARK: - Variables
    // Will be received by previous view
    var user_id = Int()
    var project_id = Int()
    var project_path = String()
    
    // Is current user admin?
    var is_admin = Bool()
    var noPhotos = false
    var FileName : NSArray = []
    var location = String()
    
    @IBOutlet weak var AddParticipant: UIButton!
    @IBOutlet weak var table: UITableView!
    
    // Display the file names
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if noPhotos{
            return 0
        }
        return FileName.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cell = UITableViewCell(style: UITableViewCell.CellStyle.default, reuseIdentifier: "cell") as UITableViewCell
        
        let item = FileName[indexPath.row] as! [String:Any]
        cell.textLabel?.text = (item["filename"] as! String)
        
        return cell
    }
    
    // Select file and move to next view (for download)
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        //Access the array that you have used to fill the tableViewCell
        let item = FileName[indexPath.row] as! [String:Any]
        
        let name = item["filename"] as! String
        let type = item["type"] as! String
 
        // create url
        var path = ""
        if let range = project_path.range(of: "p") {
            path = String(project_path[range.lowerBound...])
        }

        location = "http://54.81.239.120/" + path + "/" + type + "/" + name
        if ConnectionTest(self: self){
            // Check type of download and move to the according view
            switch type {
            case "images":
                performSegue(withIdentifier: "DownloadImage", sender: nil)
                break
            
            case "docs":
                performSegue(withIdentifier: "DownloadNotes", sender: nil)
                break
            case "voice":
                performSegue(withIdentifier: "DownloadAudio", sender: nil)
                break
            case "videos":
                performSegue(withIdentifier: "DownloadVideo", sender: nil)
                break
            default:
                break
            }
        }
    }
    
    
    // If user is not admin of project, hide add participants button
    // and download the filenames for the project
    override func viewDidLoad() {
        super.viewDidLoad()
        
        is_admin = CheckAdmin(self: self, project_id: project_id, user_id: user_id)
        if !is_admin{
            AddParticipant.isHidden = true
        }
        
        // Create Request
        
        let url = URL(string: "http://54.81.239.120/selectAPI.php");
        var request = URLRequest(url:url!)
        request.httpMethod = "POST"
        let post = "queryType=10&pid=\(project_id)";
        request.httpBody = post.data(using: String.Encoding.utf8);
        
        let response = ConnectToAPI(self: self, request: request)
        
        if (response["empty"] as? Bool ?? true) == false{
            project_path = response["path"] as! String
            fetchPhotos()
        }
        
        
        // Do any additional setup after loading the view.
    }

    // Download file names from server
    func fetchPhotos(){
        
        let _ = ConnectionTest(self: self)
        
        let url_parse = URL(string: "http://54.81.239.120/listdir.php?path=\(project_path)")
        if url_parse != nil {
            let task = URLSession.shared.dataTask(with: url_parse! as URL, completionHandler: {(data, response, error) -> Void in
                do
                {
                    let jsonRes = try JSONSerialization.jsonObject(with: data!, options: .allowFragments) as! [String: AnyObject]
                    if jsonRes["empty"] as! Bool == false{
                        self.FileName = jsonRes["files"] as! NSArray
                    }
                    DispatchQueue.main.async {
                        self.table.reloadData()
                    }
                }catch{}
                
            })
            task.resume()
        }
    }
    
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // Handles the segue and moves the data around.
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        
        if segue.identifier != "Logout"{
            let _ = ConnectionTest(self: self)
        }
        
        if (segue.identifier == "AddParticipants"){
            let vc = segue.destination as! AddParticipantViewController
            vc.user_id = user_id
            vc.project_id = project_id
        } else if (segue.identifier == "BackToDashboard"){
            let vc = segue.destination as! DashboardViewController
            vc.user_id = user_id
        } else if (segue.identifier == "AudioSegue"){
            let vc = segue.destination as! AudioViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.projectPath = project_path
        } else if (segue.identifier == "CameraSegue"){
            let vc = segue.destination as! CameraViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.projectPath = project_path

        } else if (segue.identifier == "NotesSegue"){
            let vc = segue.destination as! NotesViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.projectPath = project_path

        } else if (segue.identifier == "VideoSegue"){
            let vc = segue.destination as! VideoViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.projectPath = project_path

        } else if (segue.identifier == "DownloadNotes"){
            let vc = segue.destination as! DownloadNotesViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.location = location

        } else if (segue.identifier == "DownloadImage"){
            let vc = segue.destination as! DownloadImageViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.location = location

        } else if (segue.identifier == "DownloadVideo"){
            let vc = segue.destination as! DownloadVideoViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.location = location

        } else if (segue.identifier == "DownloadAudio"){
            let vc = segue.destination as! DownloadAudioViewController
            vc.user_id = user_id
            vc.project_id = project_id
            vc.location = location
        } else if (segue.identifier == "Logout"){
            let _ = segue.destination as! LoginViewController
        }
    }
}
