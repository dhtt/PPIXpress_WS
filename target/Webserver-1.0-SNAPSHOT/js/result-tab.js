jQuery(document).ready(function() {
    $("#ClickMe").click(function() {
        alert("In js file");
    });
    // const graph = cytoscape({
    //     container: $('#cy'),
    //     elements: [
    //         {data: {id: 'a'}},
    //         {data: {id: 'b'}},
    //         {data: {id: 'ab', source: 'a', target: 'b'}}
    //     ],
    //     style: [
    //         {
    //             selector: 'node',
    //             style: {
    //                 'background-color': '#EDF2F4',
    //                 'label': 'data(id)'
    //             }
    //         },
    //         {
    //             selector: 'edge',
    //             style: {
    //                 'width': 3,
    //                 'line-color': '#ccc',
    //                 'target-arrow-color': '#ccc',
    //                 'target-arrow-shape': 'triangle',
    //                 'curve-style': 'bezier'
    //             }
    //         }
    //     ],
    //     layout: {
    //         name: 'grid',
    //         rows: 1
    //     }
    // });
    $("#form").submit(function (){
        const form = $("form")[0];
        const data = new FormData(form);
        $.ajax({
            url: "PPIXpress",
            method: "POST",
            enctype: 'multipart/form-data',
            data: data,
            processData : false,
            contentType : false,
            success: function (resultText) {
                $('#RunningProgressContent').html(resultText)
            }
        })
        return false;
    })
});



function toggle1(name, displayTabs, chosenTab, chosenTab_contents){
    for (let i=0; i < displayTabs.length; i++){
        // Set tab as active
        displayTabs[i].classList.remove("active")

        // Show content for tab
        if (chosenTab_contents[i].getAttribute('name') !== name){
            chosenTab_contents[i].classList.add("non-display")
        }
        else {
            chosenTab_contents[i].classList.remove("non-display")
        }
    }
    chosenTab.classList.add("active")
}

function getContent(name){
    let displayTabs, chosenTab, chosenTab_contents;
    displayTabs = document.getElementsByClassName("button-tab");
    chosenTab = document.getElementById(name);
    chosenTab_contents = document.getElementsByClassName("display-content");
    toggle1(name, displayTabs, chosenTab, chosenTab_contents);
}

